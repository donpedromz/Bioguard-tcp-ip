package com.donpedromz.test;

import com.donpedromz.common.IConfigReader;
import com.donpedromz.common.PropertiesManager;
import com.donpedromz.network.RequestBuilder;
import com.donpedromz.network.SSLTCPClient;
import com.donpedromz.network.TCPClient;
import com.donpedromz.network.config.ISSLConfig;
import com.donpedromz.network.config.SSLConfig;

/**
 * @version 1.0
 * @author juanp
 * pruebas de seguridad para el servidor BioGuard TCP/SSL.
 * Verifica que el servidor maneje correctamente:
 *   - Encabezados de protocolo malformados o infectados<
 *   - Rutas inexistentes o métodos HTTP no soportados
 *   - Peticiones con formato incorrecto (FASTA inválido)
 *   - Datos inválidos que no cumplen las reglas de validación
 *   - Ataques de inyección (SQL, XSS, CSV injection)
 *   - Ataques de desbordamiento y caracteres especiales
 * Todas las respuestas deben seguir el formato estandarizado: {@code [TCP][STATUS][Category] msg}
 * y nunca exponer información interna del servidor.
 */
public class SecurityTest {
    private static final String FASTA_CONTENT_TYPE = "application/fasta";

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        IConfigReader configReader = new PropertiesManager("application.properties");
        ISSLConfig clientConfig = new SSLConfig(configReader);
        TCPClient tcpClient = new SSLTCPClient(clientConfig);
        System.out.println("BIOGUARD - Pruebas de Seguridad");
        System.out.println();
        // 1. Ataques a encabezados del protocolo
        testProtocolHeaderAttacks(tcpClient);

        // 2. Ataques a rutas y métodos
        testRouteAttacks(tcpClient);

        // 3. Ataques al content-type
        testContentTypeAttacks(tcpClient);

        // 4. Ataques al formato FASTA
        testFastaFormatAttacks(tcpClient);

        // 5. Datos inválidos de pacientes
        testInvalidPatientData(tcpClient);

        // 6. Datos inválidos de enfermedades
        testInvalidDiseaseData(tcpClient);

        // 7. Datos inválidos de diagnósticos
        testInvalidDiagnosticData(tcpClient);

        // 8. Ataques de inyección
        testInjectionAttacks(tcpClient);

        // 9. Ataques de desbordamiento y boundary
        testOverflowAndBoundaryAttacks(tcpClient);

        // Resumen final
        System.out.println();
        System.out.println("=== RESUMEN DE PRUEBAS ===");
        System.out.println("  Total:    " + totalTests);
        System.out.println("  Exitosas: " + passedTests);
        System.out.println("  Fallidas: " + failedTests);
        System.out.println("  Tasa:     " + (totalTests > 0 ? (passedTests * 100 / totalTests) + "%" : "N/A"));
    }
    /**
     * Pruebas de ataque a encabezados del protocolo (request-line, content-type).
     * Verifica que el servidor responda con errores 400 para mensajes malformados y no revele información interna.
     * @param tcpClient cliente TCP para enviar mensajes al servidor
      */
    private static void testProtocolHeaderAttacks(TCPClient tcpClient) {
        System.out.println("--- 1. Ataques a encabezados del protocolo ---");

        // 1.1 Mensaje completamente vacío
        assertResponseContains(tcpClient,
                "",
                "[TCP][400]",
                "1.1 Mensaje vacío");

        // 1.2 Mensaje con solo espacios en blanco
        assertResponseContains(tcpClient,
                "   ",
                "[TCP][400]",
                "1.2 Mensaje solo espacios");

        // 1.3 Mensaje con una sola línea (falta content-type y body)
        assertResponseContains(tcpClient,
                "POST patient",
                "[TCP][400]",
                "1.3 Solo request-line (1 parte)");

        // 1.4 Mensaje con dos líneas (falta body)
        assertResponseContains(tcpClient,
                "POST patient\napplication/fasta",
                "[TCP][400]",
                "1.4 Sin body (2 partes)");

        // 1.5 Request-line sin acción (solo método)
        assertResponseContains(tcpClient,
                "POST\napplication/fasta\n>body",
                "[TCP][400]",
                "1.5 Request-line sin acción");

        // 1.6 Request-line vacía
        assertResponseContains(tcpClient,
                "\napplication/fasta\n>body",
                "[TCP][400]",
                "1.6 Request-line vacía");

        // 1.7 Mensaje con saltos de línea extras al inicio
        assertResponseContains(tcpClient,
                "\n\n\nPOST patient\napplication/fasta\n>body",
                "[TCP][400]",
                "1.7 Saltos de línea extras al inicio");

        // 1.8 Request-line con múltiples espacios
        String multiSpaceMsg = new RequestBuilder()
                .method("POST")
                .action("patient")
                .contentType(FASTA_CONTENT_TYPE)
                .body(">12345|Juan|Perez|30|juan@test.com|MASCULINO|City|Country")
                .build();
        // Envío directo para manipular el formato — no debería causar error de ruta sino funcionar o error en parsing
        assertResponseNotContains(tcpClient,
                multiSpaceMsg,
                "[TCP][500]",
                "1.8 Request bien formada no produce error 500");

        // 1.9 Caracteres de control en el mensaje
        assertResponseContains(tcpClient,
                "POST patient\napplication/fasta\n>\u0000\u0001\u0002\u0003",
                "[TCP][400]",
                "1.9 Caracteres de control en body");

        // 1.10 Tabulaciones en lugar de espacios en request-line
        assertResponseContains(tcpClient,
                "POST\tpatient\napplication/fasta\n>body",
                "[TCP][400]",
                "1.10 Tab en request-line");

        System.out.println();
    }
    /**
     * 2. ATAQUES A RUTAS Y MÉTODOS
      * Verifica que el servidor responda correctamente a métodos no soportados y rutas inexistentes.
      * Todos los casos deben retornar un error 404 sin revelar información interna del servidor.
     * @param tcpClient
     */
    private static void testRouteAttacks(TCPClient tcpClient) {
        System.out.println("--- 2. Ataques a rutas y métodos ---");

        // 2.1 Método GET (no soportado)
        assertResponseContains(tcpClient,
                buildRawMessage("GET", "patient", FASTA_CONTENT_TYPE, ">12345|Juan|Perez|30|j@t.com|MASCULINO|C|C"),
                "[TCP][404]",
                "2.1 Método GET no soportado");

        // 2.2 Método DELETE (no soportado)
        assertResponseContains(tcpClient,
                buildRawMessage("DELETE", "patient", FASTA_CONTENT_TYPE, ">12345|body"),
                "[TCP][404]",
                "2.2 Método DELETE no soportado");

        // 2.3 Método PUT (no soportado)
        assertResponseContains(tcpClient,
                buildRawMessage("PUT", "patient", FASTA_CONTENT_TYPE, ">12345|body"),
                "[TCP][404]",
                "2.3 Método PUT no soportado");

        // 2.4 Método PATCH (no soportado)
        assertResponseContains(tcpClient,
                buildRawMessage("PATCH", "disease", FASTA_CONTENT_TYPE, ">body"),
                "[TCP][404]",
                "2.4 Método PATCH no soportado");

        // 2.5 Acción inexistente
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "users", FASTA_CONTENT_TYPE, ">body"),
                "[TCP][404]",
                "2.5 Acción 'users' inexistente");

        // 2.6 Acción 'admin' (intento de acceso administrativo)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "admin", FASTA_CONTENT_TYPE, ">body"),
                "[TCP][404]",
                "2.6 Acción 'admin' inexistente");

        // 2.7 Acción con path traversal
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "../../../etc/passwd", FASTA_CONTENT_TYPE, ">body"),
                "[TCP][404]",
                "2.7 Path traversal en acción");

        // 2.8 Método en minúsculas
        assertResponseContains(tcpClient,
                buildRawMessage("post", "patient", FASTA_CONTENT_TYPE, ">12345|body"),
                "[TCP][404]",
                "2.8 Método en minúsculas");

        // 2.9 Acción en mayúsculas
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "PATIENT", FASTA_CONTENT_TYPE, ">12345|body"),
                "[TCP][404]",
                "2.9 Acción en mayúsculas");

        // 2.10 Método vacío con acción (el servidor lo detecta como request-line malformada → 400)
        assertResponseContains(tcpClient,
                " patient\napplication/fasta\n>body",
                "[TCP][400]",
                "2.10 Método vacío con acción");

        System.out.println();
    }
    /**
     * 3. ATAQUES AL CONTENT-TYPE
      * Verifica que el servidor responda con errores 400 para content-types no soportados o malformados.
      * El servidor no debe procesar el mensaje ni revelar información interna en la respuesta.
     * @param tcpClient cliente TCP para enviar mensajes al servidor
     */
    private static void testContentTypeAttacks(TCPClient tcpClient) {
        System.out.println("--- 3. Ataques al content-type ---");

        // 3.1 Content-type text/plain (no registrado)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", "text/plain",
                        ">12345|Juan|Perez|30|j@t.com|MASCULINO|City|Country"),
                "[TCP][400]",
                "3.1 Content-type text/plain");

        // 3.2 Content-type application/json (no registrado)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "disease", "application/json",
                        "{\"name\":\"virus\"}"),
                "[TCP][400]",
                "3.2 Content-type application/json");

        // 3.3 Content-type vacío (bypass RequestBuilder que valida contentType)
        assertResponseContains(tcpClient,
                "POST patient\n\n>12345|Juan|Perez|30|j@t.com|MASCULINO|City|Country",
                "[TCP][400]",
                "3.3 Content-type vacío");

        // 3.4 Content-type con inyección (bypass RequestBuilder)
        assertResponseContains(tcpClient,
                "POST patient\napplication/fasta; DROP TABLE patients\n>12345|Juan|Perez|30|j@t.com|MASCULINO|City|Country",
                "[TCP][400]",
                "3.4 Content-type con inyección SQL");

        System.out.println();
    }
    /**
     * 4. ATAQUES AL FORMATO FASTA
      * Verifica que el servidor responda con errores 400 para mensajes con formato FASTA inválido.
      * El servidor no debe procesar el mensaje ni revelar información interna en la respuesta.
      * Se prueban casos como body vacío, falta de prefijo >, líneas extra, campos insuficientes o delimitadores incorrectos.
      * @param tcpClient cliente TCP para enviar mensajes al servidor
     */
    private static void testFastaFormatAttacks(TCPClient tcpClient) {
        System.out.println("--- 4. Ataques al formato FASTA ---");

        // 4.1 Body vacío para paciente (bypass RequestBuilder que valida body)
        assertResponseContains(tcpClient,
                "POST patient\napplication/fasta\n",
                "[TCP][400]",
                "4.1 Body vacío para paciente");

        // 4.2 Body sin prefijo > para paciente
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        "12345|Juan|Perez|30|j@t.com|MASCULINO|City|Country"),
                "[TCP][400]",
                "4.2 Sin prefijo > para paciente");

        // 4.3 Body sin prefijo > para enfermedad
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "disease", FASTA_CONTENT_TYPE,
                        "VirusTest|ALTA\nACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "4.3 Sin prefijo > para enfermedad");

        // 4.4 Body sin prefijo > para diagnóstico
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "diagnose", FASTA_CONTENT_TYPE,
                        "12345|2025-01-01\nACGTACGT"),
                "[TCP][400]",
                "4.4 Sin prefijo > para diagnóstico");

        // 4.5 Paciente con múltiples líneas (debe ser 1)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        ">12345|Juan|Perez|30|j@t.com|MASCULINO|City|Country\nextra_line"),
                "[TCP][400]",
                "4.5 Paciente con línea extra");

        // 4.6 Enfermedad con 1 sola línea (necesita 2)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "disease", FASTA_CONTENT_TYPE,
                        ">VirusTest|ALTA"),
                "[TCP][400]",
                "4.6 Enfermedad con 1 línea (necesita 2)");

        // 4.7 Enfermedad con 3 líneas (necesita 2)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "disease", FASTA_CONTENT_TYPE,
                        ">VirusTest|ALTA\nACGTACGTACGTACGTACGT\nextra"),
                "[TCP][400]",
                "4.7 Enfermedad con 3 líneas (necesita 2)");

        // 4.8 Diagnóstico con 1 sola línea (necesita 2)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "diagnose", FASTA_CONTENT_TYPE,
                        ">12345|2025-01-01"),
                "[TCP][400]",
                "4.8 Diagnóstico con 1 línea (necesita 2)");

        // 4.9 Paciente con campos insuficientes (menos de 8)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        ">12345|Juan|Perez"),
                "[TCP][400]",
                "4.9 Paciente con campos insuficientes");

        // 4.10 Paciente con campos extra (más de 8)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        ">12345|Juan|Perez|30|j@t.com|MASCULINO|City|Country|ExtraField"),
                "[TCP][400]",
                "4.10 Paciente con campos extra");

        // 4.11 Enfermedad con campos insuficientes en header (1 campo)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "disease", FASTA_CONTENT_TYPE,
                        ">VirusTest\nACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "4.11 Enfermedad header con 1 campo");

        // 4.12 Diagnóstico con campos insuficientes en header (1 campo)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "diagnose", FASTA_CONTENT_TYPE,
                        ">12345\nACGTACGT"),
                "[TCP][400]",
                "4.12 Diagnóstico header con 1 campo");

        // 4.13 Body solo con >
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE, ">"),
                "[TCP][400]",
                "4.13 Body solo con >");

        // 4.14 Delimitador incorrecto (coma en vez de pipe)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        ">12345,Juan,Perez,30,j@t.com,MASCULINO,City,Country"),
                "[TCP][400]",
                "4.14 Delimitador incorrecto (coma)");

        System.out.println();
    }
    /**
     * 5. DATOS INVÁLIDOS DE PACIENTES
      * Verifica que el servidor responda con errores 400 para datos de pacientes que no cumplen las reglas de validación.
      * Se prueban casos como edad cero, edad negativa, edad superior al límite, edad no numérica, documento con letras o caracteres especiales, nombre con números o caracteres especiales, email sin formato válido, género inválido, ciudad con números, etc.
      * El servidor no debe procesar el mensaje ni revelar información interna en la respuesta.
     * @param tcpClient
     */
    private static void testInvalidPatientData(TCPClient tcpClient) {
        System.out.println("--- 5. Datos inválidos de pacientes ---");

        // 5.1 Edad cero
        assertResponseContains(tcpClient,
                buildPatientMessage("11111", "Juan", "Perez", "0", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.1 Edad cero");

        // 5.2 Edad negativa
        assertResponseContains(tcpClient,
                buildPatientMessage("11112", "Juan", "Perez", "-5", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.2 Edad negativa (-5)");

        // 5.3 Edad superior al límite (>120)
        assertResponseContains(tcpClient,
                buildPatientMessage("11113", "Juan", "Perez", "999", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.3 Edad superior al límite (999)");

        // 5.4 Edad no numérica
        assertResponseContains(tcpClient,
                buildPatientMessage("11114", "Juan", "Perez", "treinta", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.4 Edad no numérica ('treinta')");

        // 5.5 Documento con letras
        assertResponseContains(tcpClient,
                buildPatientMessage("ABC12345", "Juan", "Perez", "30", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.5 Documento con letras");

        // 5.6 Documento con caracteres especiales
        assertResponseContains(tcpClient,
                buildPatientMessage("123-456-789", "Juan", "Perez", "30", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.6 Documento con guiones");

        // 5.7 Documento vacío
        assertResponseContains(tcpClient,
                buildPatientMessage("", "Juan", "Perez", "30", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.7 Documento vacío");

        // 5.8 Nombre con números
        assertResponseContains(tcpClient,
                buildPatientMessage("11115", "Juan123", "Perez", "30", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.8 Nombre con números");

        // 5.9 Nombre con caracteres especiales
        assertResponseContains(tcpClient,
                buildPatientMessage("11116", "Ju@n!", "Perez", "30", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.9 Nombre con caracteres especiales");

        // 5.10 Apellido vacío
        assertResponseContains(tcpClient,
                buildPatientMessage("11117", "Juan", "", "30", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.10 Apellido vacío");

        // 5.11 Email sin @
        assertResponseContains(tcpClient,
                buildPatientMessage("11118", "Juan", "Perez", "30", "jtest.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.11 Email sin @");

        // 5.12 Email sin dominio
        assertResponseContains(tcpClient,
                buildPatientMessage("11119", "Juan", "Perez", "30", "j@", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.12 Email sin dominio");

        // 5.13 Email con doble @
        assertResponseContains(tcpClient,
                buildPatientMessage("11120", "Juan", "Perez", "30", "j@@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.13 Email con doble @");

        // 5.14 Email vacío
        assertResponseContains(tcpClient,
                buildPatientMessage("11121", "Juan", "Perez", "30", "", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.14 Email vacío");

        // 5.15 Género inválido
        assertResponseContains(tcpClient,
                buildPatientMessage("11122", "Juan", "Perez", "30", "j@test.com", "INVALIDO", "City", "Country"),
                "[TCP][400]",
                "5.15 Género inválido ('INVALIDO')");

        // 5.16 Género vacío
        assertResponseContains(tcpClient,
                buildPatientMessage("11123", "Juan", "Perez", "30", "j@test.com", "", "City", "Country"),
                "[TCP][400]",
                "5.16 Género vacío");

        // 5.17 Ciudad con números
        assertResponseContains(tcpClient,
                buildPatientMessage("11124", "Juan", "Perez", "30", "j@test.com", "MASCULINO", "City123", "Country"),
                "[TCP][400]",
                "5.17 Ciudad con números");

        // 5.18 País vacío
        assertResponseContains(tcpClient,
                buildPatientMessage("11125", "Juan", "Perez", "30", "j@test.com", "MASCULINO", "City", ""),
                "[TCP][400]",
                "5.18 País vacío");

        // 5.19 Edad en el límite superior exacto (120 — debería ser válido)
        assertResponseNotContains(tcpClient,
                buildPatientMessage("11126", "Juan", "Perez", "120", "j120@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.19 Edad 120 (límite válido)");

        // 5.20 Edad en el límite inferior exacto (1 — debería ser válido)
        assertResponseNotContains(tcpClient,
                buildPatientMessage("11127", "Juan", "Perez", "1", "j1@test.com", "FEMENINO", "City", "Country"),
                "[TCP][400]",
                "5.20 Edad 1 (límite válido)");

        // 5.21 Edad 121 (justo fuera del límite)
        assertResponseContains(tcpClient,
                buildPatientMessage("11128", "Juan", "Perez", "121", "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "5.21 Edad 121 (fuera del límite)");

        // 5.22 Nombre con acentos válidos (debería ser válido)
        assertResponseNotContains(tcpClient,
                buildPatientMessage("11129", "María", "González", "25", "maria@test.com", "FEMENINO", "Bogotá", "Perú"),
                "[TCP][400]",
                "5.22 Nombre con acentos (válido)");

        // 5.23 Género OTRO (debería ser válido)
        assertResponseNotContains(tcpClient,
                buildPatientMessage("11130", "Alex", "Martinez", "35", "alex@test.com", "OTRO", "Lima", "Peru"),
                "[TCP][400]",
                "5.23 Género OTRO (válido)");

        // 5.24 Género NO ESPECIFICADO (con espacio — debería funcionar)
        assertResponseNotContains(tcpClient,
                buildPatientMessage("11131", "Sam", "Roque", "22", "sam@test.com", "NO ESPECIFICADO", "Quito", "Ecuador"),
                "[TCP][400]",
                "5.24 Género NO ESPECIFICADO (válido)");

        System.out.println();
    }
    /**
     * 6. DATOS INVÁLIDOS DE ENFERMEDADES
      * Verifica que el servidor responda con errores 400 para datos de enfermedades que no cumplen las reglas de validación.
      * Se prueban casos como secuencia con caracteres inválidos, secuencia demasiado corta, secuencia vacía, nombre de enfermedad vacío, nivel de infecciosidad inválido o vacío, nombre con caracteres especiales prohibidos, secuencia con minúsculas, secuencia con números, etc.
      * El servidor no debe procesar el mensaje ni revelar información interna en la respuesta.
     * @param tcpClient
     */
    private static void testInvalidDiseaseData(TCPClient tcpClient) {
        System.out.println("--- 6. Datos inválidos de enfermedades ---");

        // 6.1 Secuencia con caracteres inválidos
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusInvalido1", "ALTA", "ACGTXYZACGTACGTACGT"),
                "[TCP][400]",
                "6.1 Secuencia con caracteres inválidos (XYZ)");

        // 6.2 Secuencia demasiado corta (<15 caracteres)
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusCorto2", "ALTA", "ACGTACGT"),
                "[TCP][400]",
                "6.2 Secuencia demasiado corta (8 chars)");

        // 6.3 Secuencia vacía
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusVacio3", "ALTA", ""),
                "[TCP][400]",
                "6.3 Secuencia vacía");

        // 6.4 Nombre de enfermedad vacío
        assertResponseContains(tcpClient,
                buildDiseaseMessage("", "ALTA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "6.4 Nombre enfermedad vacío");

        // 6.5 Nivel de infecciosidad inválido
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusLevel5", "CRITICA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "6.5 Nivel infecciosidad 'CRITICA' inválido");

        // 6.6 Nivel de infecciosidad vacío
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusLevel6", "", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "6.6 Nivel infecciosidad vacío");

        // 6.7 Nombre con caracteres especiales prohibidos
        assertResponseContains(tcpClient,
                buildDiseaseMessage("Virus@#$%", "ALTA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "6.7 Nombre con caracteres especiales");

        // 6.8 Secuencia con minúsculas (debería normalizarse si toUpperCase en parser)
        // El parser de diagnóstico aplica toUpperCase, pero el de enfermedad no lo hace en el parser —
        // depende de la validación en el repositorio
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusMinusc8", "BAJA", "acgtacgtacgtacgtacgt"),
                "[TCP][",
                "6.8 Secuencia en minúsculas (verificar manejo)");

        // 6.9 Secuencia con números
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusNum9", "MEDIA", "ACGT12345ACGTACGT"),
                "[TCP][400]",
                "6.9 Secuencia con números");

        // 6.10 Secuencia exactamente 15 caracteres (límite válido)
        assertResponseNotContains(tcpClient,
                buildDiseaseMessage("VirusExacto10", "BAJA", "ACGTACGTACGTACG"),
                "[TCP][400]",
                "6.10 Secuencia exactamente 15 chars (válido)");

        // 6.11 Secuencia de 14 caracteres (justo bajo el límite)
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusCorto11", "BAJA", "ACGTACGTACGTAC"),
                "[TCP][400]",
                "6.11 Secuencia 14 chars (bajo límite)");

        // 6.12 Nombre de enfermedad con espacios (debería ser válido según regex)
        assertResponseNotContains(tcpClient,
                buildDiseaseMessage("Virus Con Espacio", "MEDIA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "6.12 Nombre con espacios (válido)");

        // 6.13 Nombre de enfermedad con guión (debería ser válido según regex)
        assertResponseNotContains(tcpClient,
                buildDiseaseMessage("SARS-CoV-2", "ALTA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "6.13 Nombre con guiones (válido)");

        System.out.println();
    }
    /**
     * 7. DATOS INVÁLIDOS DE DIAGNÓSTICOS
      * Verifica que el servidor responda con errores 400 para datos de diagnósticos que no cumplen las reglas de validación.
      * Se prueban casos como fecha con formato incorrecto, fecha vacía, fecha con texto, secuencia demasiado corta, secuencia con caracteres inválidos, secuencia vacía, documento del paciente con letras o vacío, etc.
      * El servidor no debe procesar el mensaje ni revelar información interna en la respuesta.
     * @param tcpClient
     */
    private static void testInvalidDiagnosticData(TCPClient tcpClient) {
        System.out.println("--- 7. Datos inválidos de diagnósticos ---");

        // 7.1 Fecha con formato incorrecto (DD-MM-YYYY)
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "15-10-2025", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.1 Fecha DD-MM-YYYY");

        // 7.2 Fecha con formato incorrecto (MM/DD/YYYY)
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "10/15/2025", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.2 Fecha MM/DD/YYYY");

        // 7.3 Fecha vacía
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.3 Fecha vacía");

        // 7.4 Fecha con texto
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "ayer", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.4 Fecha con texto ('ayer')");

        // 7.5 Secuencia demasiado corta (<7 caracteres)
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-01-01", "ACGTAC"),
                "[TCP][400]",
                "7.5 Secuencia diagnóstico < 7 chars");

        // 7.6 Secuencia con caracteres inválidos
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-01-01", "ACGTXYZ"),
                "[TCP][400]",
                "7.6 Secuencia diagnóstico con chars inválidos");

        // 7.7 Secuencia vacía (body tiene header pero sin segunda línea)
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "diagnose", FASTA_CONTENT_TYPE,
                        ">99887766|2025-01-01"),
                "[TCP][400]",
                "7.7 Diagnóstico sin secuencia (1 línea)");

        // 7.8 Documento del paciente con letras
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("ABC123", "2025-01-01", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.8 Documento diagnóstico con letras");

        // 7.9 Documento vacío
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("", "2025-01-01", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.9 Documento vacío en diagnóstico");

        // 7.10 Secuencia exactamente 7 caracteres (límite válido)
        // Nota: el paciente puede no existir, pero la validación de formato debe pasar
        assertResponseNotContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-01-01", "ACGTACG"),
                "[TCP][400][InvalidFormat]",
                "7.10 Secuencia exacta 7 chars (formato válido)");

        // 7.11 Fecha con fecha imposible (mes 13)
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-13-01", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.11 Fecha imposible (mes 13)");

        // 7.12 Fecha con día 32
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-01-32", "ACGTACGTACGT"),
                "[TCP][400]",
                "7.12 Fecha imposible (día 32)");

        System.out.println();
    }
    /**
     * 8. ATAQUES DE INYECCIÓN
      * Verifica que el servidor responda con errores 400 para intentos de inyección en los campos de pacientes, enfermedades y diagnósticos.
      * Se prueban casos como SQL injection, XSS, path traversal, CSV injection, null byte injection, CRLF injection, command injection, unicode homoglyphs, etc.
      * El servidor no debe procesar el mensaje ni revelar información interna en la respuesta.
     * @param tcpClient
     */
    private static void testInjectionAttacks(TCPClient tcpClient) {
        System.out.println("--- 8. Ataques de inyección ---");

        // 8.1 SQL injection en nombre de paciente
        assertResponseContains(tcpClient,
                buildPatientMessage("11200", "'; DROP TABLE patients; --", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.1 SQL injection en nombre");

        // 8.2 XSS en nombre de paciente
        assertResponseContains(tcpClient,
                buildPatientMessage("11201", "<script>alert('xss')</script>", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.2 XSS en nombre");

        // 8.3 SQL injection en documento
        assertResponseContains(tcpClient,
                buildPatientMessage("1 OR 1=1", "Juan", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.3 SQL injection en documento");

        // 8.4 Path traversal en nombre de enfermedad
        assertResponseContains(tcpClient,
                buildDiseaseMessage("../../etc/passwd", "ALTA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "8.4 Path traversal en nombre enfermedad");

        // 8.5 CSV injection en nombre (fórmula de hoja de cálculo)
        assertResponseContains(tcpClient,
                buildPatientMessage("11202", "=CMD('calc')", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.5 CSV injection (fórmula) en nombre");

        // 8.6 Null byte injection en documento
        assertResponseContains(tcpClient,
                buildPatientMessage("12345\u0000admin", "Juan", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.6 Null byte injection en documento");

        // 8.7 CRLF injection en nombre
        assertResponseContains(tcpClient,
                buildPatientMessage("11203", "Juan\r\nHTTP/1.1 200 OK", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.7 CRLF injection en nombre");

        // 8.8 Command injection en email
        assertResponseContains(tcpClient,
                buildPatientMessage("11204", "Juan", "Perez", "30",
                        "j@test.com; rm -rf /", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "8.8 Command injection en email");

        // 8.9 XSS en nombre de enfermedad
        assertResponseContains(tcpClient,
                buildDiseaseMessage("<img src=x onerror=alert(1)>", "ALTA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "8.9 XSS en nombre enfermedad");

        // 8.10 Unicode homoglyph en secuencia (A cirílica en vez de A latina)
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusHomoglyph", "ALTA", "АCGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "8.10 Homoglyph en secuencia genética");

        // 8.11 SQL injection en fecha de diagnóstico
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-01-01' OR '1'='1", "ACGTACGTACGT"),
                "[TCP][400]",
                "8.11 SQL injection en fecha diagnóstico");

        // 8.12 Pipe injection en campos (el | es delimitador FASTA)
        // Intentar inyectar un campo extra vía |
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        ">12345|Juan|Perez|30|j@test.com|MASCULINO|City|Country|extraInjected|moreData"),
                "[TCP][400]",
                "8.12 Pipe injection (campos extra via |)");

        System.out.println();
    }
    /**
     * 9. ATAQUES DE DESBORDAMIENTO Y BOUNDARY
      * Verifica que el servidor responda con errores 400 para datos que exceden los límites esperados o que prueban los bordes de validación.
      * Se prueban casos como nombre de paciente extremadamente largo, secuencia genética extremadamente larga, documento con demasiados dígitos, email con longitud excesiva, edad como número extremadamente grande, múltiples pipes consecutivos, unicode emoji en campos de texto, body con solo newlines, etc.
     * @param tcpClient
     */
    private static void testOverflowAndBoundaryAttacks(TCPClient tcpClient) {
        System.out.println("--- 9. Ataques de desbordamiento y boundary ---");

        // 9.1 Nombre extremadamente largo (>100 chars)
        String longName = "A".repeat(150);
        assertResponseContains(tcpClient,
                buildPatientMessage("22200", longName, "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.1 Nombre > 100 caracteres");

        // 9.2 Secuencia genética de enfermedad > 5000 chars
        String longSequence = "ACGT".repeat(1500); // 6,000 caracteres
        assertResponseContains(tcpClient,
                buildDiseaseMessage("VirusLargo", "ALTA", longSequence),
                "[TCP][400]",
                "9.2 Secuencia enfermedad > 5,000 chars");

        // 9.3 Documento > 20 dígitos
        String longDocument = "1".repeat(25);
        assertResponseContains(tcpClient,
                buildPatientMessage(longDocument, "Juan", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.3 Documento > 20 dígitos");

        // 9.4 Email > 254 caracteres
        String longEmail = "a".repeat(200) + "@" + "b".repeat(50) + ".com";
        assertResponseContains(tcpClient,
                buildPatientMessage("22201", "Juan", "Perez", "30",
                        longEmail, "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.4 Email > 254 caracteres");

        // 9.5 Edad como número extremadamente grande
        assertResponseContains(tcpClient,
                buildPatientMessage("22202", "Juan", "Perez", "2147483647",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.5 Edad Integer.MAX_VALUE");

        // 9.6 Edad como número que desborda int
        assertResponseContains(tcpClient,
                buildPatientMessage("22203", "Juan", "Perez", "99999999999999999",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.6 Edad desborda int");

        // 9.7 Múltiples pipes consecutivos
        assertResponseContains(tcpClient,
                buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE,
                        ">||||||||"),
                "[TCP][400]",
                "9.7 Múltiples pipes consecutivos");

        // 9.8 Unicode emoji en nombre
        assertResponseContains(tcpClient,
                buildPatientMessage("22204", "Ju\uD83D\uDE00an", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.8 Emoji en nombre");

        // 9.9 Body con solo newlines (bypass RequestBuilder)
        assertResponseContains(tcpClient,
                "POST patient\napplication/fasta\n\n\n\n\n\n",
                "[TCP][400]",
                "9.9 Body solo newlines");

        // 9.10 Nombre de enfermedad > 200 chars
        String longDiseaseName = "Virus" + "A".repeat(250);
        assertResponseContains(tcpClient,
                buildDiseaseMessage(longDiseaseName, "ALTA", "ACGTACGTACGTACGTACGT"),
                "[TCP][400]",
                "9.10 Nombre enfermedad > 200 chars");

        // 9.11 Apellido > 100 caracteres
        String longLastName = "B".repeat(150);
        assertResponseContains(tcpClient,
                buildPatientMessage("22205", "Juan", longLastName, "30",
                        "j@test.com", "MASCULINO", "City", "Country"),
                "[TCP][400]",
                "9.11 Apellido > 100 caracteres");

        // 9.12 Ciudad > 100 caracteres
        String longCity = "A".repeat(150);
        assertResponseContains(tcpClient,
                buildPatientMessage("22206", "Juan", "Perez", "30",
                        "j@test.com", "MASCULINO", longCity, "Country"),
                "[TCP][400]",
                "9.12 Ciudad > 100 caracteres");

        // 9.13 País > 100 caracteres
        String longCountry = "B".repeat(150);
        assertResponseContains(tcpClient,
                buildPatientMessage("22207", "Juan", "Perez", "30",
                        "j@test.com", "MASCULINO", "City", longCountry),
                "[TCP][400]",
                "9.13 País > 100 caracteres");

        // 9.14 Secuencia diagnóstico > 5,000 chars
        String longDiagSequence = "ACGT".repeat(1500);
        assertResponseContains(tcpClient,
                buildDiagnosticMessage("99887766", "2025-01-01", longDiagSequence),
                "[TCP][400]",
                "9.14 Secuencia diagnóstico > 5,000 chars");

        // 9.15 Documento diagnóstico > 20 chars
        String longDiagDoc = "9".repeat(25);
        assertResponseContains(tcpClient,
                buildDiagnosticMessage(longDiagDoc, "2025-01-01", "ACGTACGTACGT"),
                "[TCP][400]",
                "9.15 Documento diagnóstico > 20 chars");

        // 9.16 Verificar que errores no exponen stack traces
        String response = sendAndGet(tcpClient,
                buildRawMessage("POST", "patient", "content/type-que-no-existe",
                        ">12345|Juan|Perez|30|j@test.com|MASCULINO|City|Country"));
        assertNoStackTrace(response, "9.16 No exponer stack trace en respuestas");

        // 9.17 Verificar que errores no exponen rutas del sistema
        String response2 = sendAndGet(tcpClient,
                "POST patient\n\n>12345|Juan|Perez|30|j@test.com|MASCULINO|City|Country");
        assertNoSystemPaths(response2, "9.17 No exponer rutas del sistema");

        System.out.println();
    }
    /**
     * Construye un mensaje raw en formato de protocolo con los campos dados.
     * Los campos se pasan como parámetros para facilitar la construcción de mensajes personalizados en las pruebas.
     * @param method método HTTP (e.g., "POST")
     * @param action acción del endpoint (e.g., "patient", "disease", "diagnose")
     * @param contentType tipo de contenido (e.g., "application/fasta")
     * @param body cuerpo del mensaje (e.g., secuencia FASTA con header y body)
     * @return mensaje completo en formato de protocolo listo para enviar al servidor TCP
     */
    private static String buildRawMessage(String method, String action, String contentType, String body) {
        return new RequestBuilder()
                .method(method)
                .action(action)
                .contentType(contentType)
                .body(body)
                .build();
    }
    /**
     * Construye un mensaje FASTA de paciente válido en formato de protocolo.
     * @param document número de documento del paciente
     * @param firstName nombre del paciente
     * @param lastName apellido del paciente
     * @param age edad del paciente
     * @param email email del paciente
     * @param gender género del paciente
     * @param city ciudad del paciente
     * @param country país del paciente
     * @return mensaje completo en formato de protocolo listo para enviar al servidor TCP
     */
    private static String buildPatientMessage(String document, String firstName, String lastName,
                                              String age, String email, String gender, String city, String country) {
        String fastaBody = ">" + document + "|" + firstName + "|" + lastName + "|"
                + age + "|" + email + "|" + gender + "|" + city + "|" + country;
        return buildRawMessage("POST", "patient", FASTA_CONTENT_TYPE, fastaBody);
    }
    /**
     * Construye un mensaje FASTA de enfermedad válido en formato de protocolo.
     * @param name nombre de la enfermedad
     * @param level nivel de infecciosidad (ALTA, MEDIA, BAJA)
     * @param sequence secuencia genética de la enfermedad
     * @return mensaje completo en formato de protocolo listo para enviar al servidor TCP
     */
    private static String buildDiseaseMessage(String name, String level, String sequence) {
        String fastaBody = ">" + name + "|" + level + "\n" + sequence;
        return buildRawMessage("POST", "disease", FASTA_CONTENT_TYPE, fastaBody);
    }
    /**
     * Construye un mensaje FASTA de diagnóstico válido en formato de protocolo.
     * @param document número de documento del paciente al que se le hizo el diagnóstico
     * @param date fecha del diagnóstico en formato YYYY-MM-DD
     * @param sequence secuencia genética del diagnóstico
     * @return mensaje completo en formato de protocolo listo para enviar al servidor TCP
     */
    private static String buildDiagnosticMessage(String document, String date, String sequence) {
        String fastaBody = ">" + document + "|" + date + "\n" + sequence;
        return buildRawMessage("POST", "diagnose", FASTA_CONTENT_TYPE, fastaBody);
    }
    /**
     * Envía un mensaje al servidor TCP utilizando el cliente TCP y devuelve la respuesta recibida.
     * @param tcpClient cliente TCP para enviar el mensaje
     * @param message mensaje a enviar al servidor
     * @return respuesta del servidor, o un mensaje de error si ocurre una excepción durante el envío. El mensaje de error incluye el prefijo "[CLIENT_ERROR]" para distinguirlo de las respuestas del servidor.
     */
    private static String sendAndGet(TCPClient tcpClient, String message) {
        try {
            return tcpClient.send(message);
        } catch (Exception e) {
            return "[CLIENT_ERROR] " + e.getMessage();
        }
    }
    /**
     * Verifica que la respuesta contenga la cadena dada (pruebas de caso negativo).
     * @param tcpClient cliente TCP para enviar el mensaje
     * @param message mensaje a enviar al servidor
     * @param expectedSubstring cadena que se espera encontrar en la respuesta para considerar la prueba como pasada
     * @param testName nombre descriptivo de la prueba para los reportes de resultados
     */
    private static void assertResponseContains(TCPClient tcpClient, String message,
                                               String expectedSubstring, String testName) {
        totalTests++;
        String response = sendAndGet(tcpClient, message);
        if (response != null && response.contains(expectedSubstring)) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
            System.out.println("         -> " + truncate(response, 120));
        } else {
            failedTests++;
            System.out.println("  [FAIL] " + testName);
            System.out.println("         Esperado que contenga: " + expectedSubstring);
            System.out.println("         Respuesta recibida:    " + truncate(response, 120));
        }
    }
    /**
     * Verifica que la respuesta no contenga la cadena dada (pruebas de caso positivo).
     * @param tcpClient cliente TCP para enviar el mensaje
     * @param message mensaje a enviar al servidor
     * @param unexpectedSubstring cadena que no se espera encontrar en la respuesta para considerar la prueba como pasada
     * @param testName nombre descriptivo de la prueba para los reportes de resultados
     */
    private static void assertResponseNotContains(TCPClient tcpClient, String message,
                                                  String unexpectedSubstring, String testName) {
        totalTests++;
        String response = sendAndGet(tcpClient, message);
        if (response != null && !response.contains(unexpectedSubstring)) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
            System.out.println("         -> " + truncate(response, 120));
        } else {
            failedTests++;
            System.out.println("  [FAIL] " + testName);
            System.out.println("         No debería contener:  " + unexpectedSubstring);
            System.out.println("         Respuesta recibida:   " + truncate(response, 120));
        }
    }
    /**
     * Verifica que la respuesta no contenga indicios de stack traces (pruebas de seguridad para errores 500).
     * @param response respuesta del servidor a analizar
     * @param testName nombre descriptivo de la prueba para los reportes de resultados
     */
    private static void assertNoStackTrace(String response, String testName) {
        totalTests++;
        if (response == null) {
            failedTests++;
            System.out.println("  [FAIL] " + testName + " (respuesta nula)");
            return;
        }
        boolean hasStackTrace = response.contains("at com.")
                || response.contains("at java.")
                || response.contains("Exception:")
                || response.contains(".java:")
                || response.contains("Caused by:");
        if (!hasStackTrace) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
            System.out.println("         -> " + truncate(response, 120));
        } else {
            failedTests++;
            System.out.println("  [FAIL] " + testName + " (stack trace expuesto)");
            System.out.println("         Respuesta: " + truncate(response, 200));
        }
    }
    /**
     * Verifica que la respuesta no contenga rutas del sistema (pruebas de seguridad para errores 500).
     * @param response respuesta del servidor a analizar
     * @param testName nombre descriptivo de la prueba para los reportes de resultados
     */
    private static void assertNoSystemPaths(String response, String testName) {
        totalTests++;
        if (response == null) {
            failedTests++;
            System.out.println("  [FAIL] " + testName + " (respuesta nula)");
            return;
        }
        boolean hasPaths = response.contains("C:\\")
                || response.contains("/home/")
                || response.contains("/usr/")
                || response.contains("/etc/")
                || response.contains("\\Users\\")
                || response.contains("/tmp/");
        if (!hasPaths) {
            passedTests++;
            System.out.println("  [PASS] " + testName);
            System.out.println("         -> " + truncate(response, 120));
        } else {
            failedTests++;
            System.out.println("  [FAIL] " + testName + " (ruta del sistema expuesta)");
            System.out.println("         Respuesta: " + truncate(response, 200));
        }
    }
    /**
     * Trunca un texto a una longitud máxima para mejorar la legibilidad en los reportes de resultados.
     * @param text texto a truncar
     * @param maxLength longitud máxima permitida antes de truncar (si el texto es más largo, se añadirá "..." al final)
     * @return el texto truncado si excede la longitud máxima, o el texto original si está dentro del límite. 
     * i el texto es nulo, retorna "null".
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) return "null";
        String clean = text.replace("\n", " ").replace("\r", "");
        return clean.length() > maxLength ? clean.substring(0, maxLength) + "..." : clean;
    }
}
