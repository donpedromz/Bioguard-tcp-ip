package com.donpedromz.data.patient;

import com.donpedromz.data.FastaUtils;
import com.donpedromz.data.exceptions.ConflictException;
import com.donpedromz.data.exceptions.CorruptedDataException;
import com.donpedromz.data.exceptions.PersistenceException;
import com.donpedromz.data.exceptions.ValidationException;
import com.donpedromz.entities.Gender;
import com.donpedromz.entities.Patient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author juanp
 * @version 1.0
 * Repositorio de pacientes que persiste datos en un archivo CSV 
 * con validación de integridad y manejo de datos corruptos.
 */
public class CSVPatientRepository implements IPatientRepository {
    /**
     * Encabezado CSV estándar para el archivo de pacientes, garantizando consistencia en la estructura de datos.
     */
    private static final String FILE_HEADER = "patientUuid,patientDocument,firstName,lastName,age,email,gender,city,country";
    /**
     * Rango de edad permitido para pacientes.
     */
    private static final int MIN_PATIENT_AGE = 1;
    private static final int MAX_PATIENT_AGE = 120;
    /**
     * Regex que identifica caracteres de control y especiales que podrían corromper el formato CSV, 
     * reemplazándolos por espacios para mantener la integridad del archivo.
     */
    private static final String CONTROL_AND_CSV_SPECIAL_CHARS_REGEX = "[\\r\\n\\t\\f\\u0000-\\u001F\\u007F,\"]";
    /**
     * Regex que detecta secuencias de múltiples espacios para normalizar a un solo espacio, 
     * evitando datos con formato inconsistente.
     */
    private static final String MULTIPLE_SPACES_REGEX = "\\s{2,}";
    /**
     * Regex para validar que el documento del paciente contenga solo dígitos, 
     * asegurando un formato básico de identificación.
     */
    private static final String DOCUMENT_REGEX = "^\\d+$";
    /**
     * Regex para validar que los nombres y apellidos contengan solo letras 
     * (incluyendo acentos y caracteres comunes en nombres)
     */
    private static final String PERSON_NAME_REGEX = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+(?: [A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$";
    /**
     * Regex para validar que las ciudades y países contengan solo letras y espacios,
     */
    private static final String LOCATION_REGEX = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+(?: [A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$";

    /**
     * Regex para validar que el correo electrónico tenga un formato básico válido.
     */
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    /**
     * Objeto de bloqueo global para sincronizar el acceso al archivo CSV, 
     * garantizando que solo un hilo pueda leer o escribir en el archivo a la vez,
     */
    private static final Object GLOBAL_FILE_LOCK = new Object();
    /**
     * Ruta absoluta y normalizada del archivo CSV utilizado para almacenar los datos de pacientes,
     */
    private final Path filePath;
    /**
     * Indicador para controlar la impresión de información de inicio en consola, 
     * asegurando que se imprima solo una vez durante la vida del repositorio.
     */
    private boolean startupInfoPrinted;
    /**
     * Crea un repositorio de pacientes usando configuración de almacenamiento CSV, 
     * extrayendo la ruta del archivo desde la configuración proporcionada.
     * @param storageConfig configuración con la ruta del CSV de pacientes
     */
    public CSVPatientRepository(IPatientStorageConfig storageConfig) {
        String filePath = storageConfig.getPatientStoragePath();
        this.filePath = Paths.get(filePath).toAbsolutePath().normalize();
        this.startupInfoPrinted = false;
        initializeCsvFile();
    }
    /**
     * Persiste un paciente en el CSV validando integridad de datos y unicidad de documento.
     * @param entity paciente a persistir
     * @throws ValidationException cuando los datos son inválidos
     * @throws ConflictException cuando el documento ya existe
     * @throws PersistenceException cuando ocurre un error de acceso a almacenamiento
     */
    @Override
    public void save(Patient entity) {
        synchronized (GLOBAL_FILE_LOCK) {
            validatePatientForPersistence(entity);
            initializeCsvFile();
            List<Patient> existingPatients = readAllPatients();
            for (Patient existing : existingPatients) {
                if (existing.getPatientDocument().equals(entity.getPatientDocument())) {
                    throw new ConflictException("Ya existe un paciente registrado con el documento: " + entity.getPatientDocument());
                }
            }
            String row = buildCsvRow(entity);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                writer.write(row);
                writer.newLine();
            } catch (IOException exception) {
                throw new PersistenceException("Error al guardar paciente en CSV", exception);
            }
        }
    }

    /**
     * Busca un paciente por su documento, leyendo el CSV línea por línea y manejando datos corruptos de forma resiliente.
     * @param document documento del paciente a buscar
     * @return paciente encontrado o {@code null} si no existe o si el documento es inválido
     */
    @Override
    public Patient getByDocument(String document) {
        synchronized (GLOBAL_FILE_LOCK) {
            if (document == null || document.isBlank()) {
                return null;
            }
            initializeCsvFile();
            List<Patient> patients = readAllPatients();
            for (Patient patient : patients) {
                if (document.trim().equals(patient.getPatientDocument())) {
                    return patient;
                }
            }
            return null;
        }
    }
    /**
     * Lee todas las filas válidas del archivo CSV y las mapea a entidades {@link Patient},
     * omitiendo filas corruptas o con formato inválido.
     * @return lista de pacientes válidos leídos del archivo CSV
     * @throws PersistenceException si ocurre un error de E/S al leer el archivo
     */
    private List<Patient> readAllPatients() {
        List<Patient> patients = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            long lineNumber = 0L;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    patients.add(mapToPatient(line, lineNumber));
                } catch (CorruptedDataException corruptedException) {
                    System.out.println("[TCP][CorruptedData] " + corruptedException.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new PersistenceException("Error al leer pacientes desde CSV", exception);
        }
        return patients;
    }
    /**
     * Inicializa el archivo CSV, asegurando carpeta y encabezado.
     */
    private void initializeCsvFile() {
        synchronized (GLOBAL_FILE_LOCK) {
            try {
                ensureParentDirectoryExists();
                boolean fileCreated = createCsvFileIfMissingOrEmpty();
                List<String> lines = readCsvFileLines();
                ensureCsvHeader(lines);
                printStartupInfoIfNeeded(fileCreated);
            } catch (IOException exception) {
                throw new PersistenceException("Error al inicializar archivo CSV de pacientes", exception);
            }
        }
    }
    /**
     * Construye la fila CSV serializada para un paciente dado, 
     * sanitizando los campos para evitar corrupción del formato.
     * @param patient paciente a serializar
     * @return fila CSV lista para ser escrita
     */
    private String buildCsvRow(Patient patient) {
        StringBuilder rowBuilder = new StringBuilder();
        appendCsvValue(rowBuilder, patient.getUuid());
        appendCsvValue(rowBuilder, patient.getPatientDocument());
        appendCsvValue(rowBuilder, patient.getFirstName());
        appendCsvValue(rowBuilder, patient.getLastName());
        appendCsvValue(rowBuilder, patient.getEdad());
        appendCsvValue(rowBuilder, patient.getEmail());
        appendCsvValue(rowBuilder, patient.getGender());
        appendCsvValue(rowBuilder, patient.getCity());
        appendCsvValue(rowBuilder, patient.getCountry());
        return rowBuilder.toString();
    }
    /**
     * Valida y normaliza los datos del paciente previo a su persistencia aplica reglas de formato, 
     * rangos y unicidad, lanzando excepciones detalladas en caso de datos inválidos.
     * @param patient paciente a validar
     * @throws ValidationException cuando uno o más campos son inválidos
     */
    private void validatePatientForPersistence(Patient patient) {
        if (patient == null) {
            throw new ValidationException("Patient no puede ser null");
        }

        UUID patientUuid = patient.getUuid();
        if (patientUuid == null) {
            patientUuid = UUID.randomUUID();
        }

        String patientDocument = FastaUtils.trimOrEmpty(patient.getPatientDocument());
        String firstName = FastaUtils.trimOrEmpty(patient.getFirstName());
        String lastName = FastaUtils.trimOrEmpty(patient.getLastName());
        String email = FastaUtils.trimOrEmpty(patient.getEmail());
        String gender = FastaUtils.trimOrEmpty(patient.getGender());
        String city = FastaUtils.trimOrEmpty(patient.getCity());
        String country = FastaUtils.trimOrEmpty(patient.getCountry());
        int sanitizedAge = patient.getEdad();

        List<String> invalidFields = new ArrayList<>();
        if (patientDocument.isEmpty() || !patientDocument.matches(DOCUMENT_REGEX)) {
            invalidFields.add("patientDocument");
        }
        if (firstName.isEmpty() || !firstName.matches(PERSON_NAME_REGEX)) {
            invalidFields.add("firstName");
        }
        if (lastName.isEmpty() || !lastName.matches(PERSON_NAME_REGEX)) {
            invalidFields.add("lastName");
        }
        if (email.isEmpty() || !isValidEmail(email)) {
            invalidFields.add("email");
        }
        if (gender.isEmpty()) {
            invalidFields.add("gender");
        }

        Gender normalizedGender = null;
        if (!gender.isEmpty()) {
            try {
                normalizedGender = Gender.fromValue(gender);
            } catch (RuntimeException exception) {
                invalidFields.add("gender");
            }
        }
        if (city.isEmpty() || !city.matches(LOCATION_REGEX)) {
            invalidFields.add("city");
        }
        if (country.isEmpty() || !country.matches(LOCATION_REGEX)) {
            invalidFields.add("country");
        }
        if (sanitizedAge < MIN_PATIENT_AGE || sanitizedAge > MAX_PATIENT_AGE) {
            invalidFields.add("age");
        }

        if (!invalidFields.isEmpty()) {
            throw new ValidationException("Campos inválidos: " + String.join(", ", invalidFields));
        }

        patient.setUuid(patientUuid);
        patient.setPatientDocument(patientDocument);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setEdad(sanitizedAge);
        patient.setEmail(email);
        patient.setGender(normalizedGender);
        patient.setCity(city);
        patient.setCountry(country);
    }

    /**
     * Agrega un valor al constructor de la fila CSV, 
     * aplicando sanitización para evitar caracteres que puedan corromper el formato del archivo.
     * @param rowBuilder constructor de la fila CSV en construcción
     * @param value valor a agregar, que será convertido a cadena y sanitizado
     */
    private void appendCsvValue(StringBuilder rowBuilder, Object value) {
        if (!rowBuilder.isEmpty()) {
            rowBuilder.append(',');
        }
        rowBuilder.append(sanitizeForCsvField(value));
    }
    /**
     * Sanitiza un valor para su inclusión segura en un campo CSV.
     * @param value valor a sanitizar, que será convertido a cadena y limpiado de caracteres problemáticos
     * @return cadena sanitizada, con caracteres de control y especiales reemplazados por espacios,
     */
    private String sanitizeForCsvField(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String clean = text.replaceAll(CONTROL_AND_CSV_SPECIAL_CHARS_REGEX, " ");
        clean = clean.replaceAll(MULTIPLE_SPACES_REGEX, " ").trim();
        return clean;
    }
    /**
     * Parsea una línea CSV soportando valores entrecomillados.
     * @param line línea CSV de entrada
     * @return arreglo de columnas parseadas
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue.setLength(0);
            } else {
                currentValue.append(currentChar);
            }
        }
        values.add(currentValue.toString());
        return values.toArray(new String[0]);
    }

    /**
     * Mapea una línea CSV a una entidad {@link Patient}.
     * @param line línea CSV de paciente
     * @return paciente mapeado o {@code null} si la línea es inválida
     */
    private Patient mapToPatient(String line, long lineNumber) {
        String[] columns = parseCsvLine(line);
        if (columns.length != 9) {
            String info = "line=" + lineNumber + 
                    " reason=Formato inválido: se esperaban exactamente 9 columnas y se encontraron " + columns.length + " raw='" + line + "'";
            System.out.println("[TCP][CorruptedData] " + info);
            throw new CorruptedDataException("Fila corrupta en CSV de pacientes: " + info);
        }
        try {
            UUID patientUuid = UUID.fromString(columns[0].trim());
            return getPatient(columns, patientUuid);
        } catch (RuntimeException exception) {
            String info = "line=" + lineNumber +
                    " reason=Error al parsear/mapear valores de la fila raw='" + line + "'";
            System.out.println("[TCP][CorruptedData] " + info);
            throw new CorruptedDataException("Fila corrupta en CSV de pacientes: " + info, exception);
        }
    }

    /**
     * Construye un objeto {@link Patient} a partir de las columnas parseadas del CSV,
     * @param columns arreglo de columnas parseadas,
     *               que se asume tienen el formato correcto y han sido validadas previamente
     * @param patientUuid UUID del paciente, que se espera sea un valor válido ya parseado desde la primera columna
     * @return instancia de {@link Patient} construida a partir de las columnas del CSV
     */
    private static Patient getPatient(String[] columns, UUID patientUuid) {
        String patientDocument = columns[1].trim();
        String firstName = columns[2].trim();
        String lastName = columns[3].trim();
        int age = Integer.parseInt(columns[4].trim());
        String email = columns[5].trim();
        String gender = columns[6].trim();
        String city = columns[7].trim();
        String country = columns[8].trim();
        return new Patient(
                patientUuid,
                patientDocument,
                firstName,
                lastName,
                age,
                email,
                gender,
                city,
                country
        );
    }



    /**
     * Valida el formato del correo electrónico.
     * @param email correo a validar
     * @return {@code true} si cumple el patrón; {@code false} en caso contrario
     */
    private boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /**
     * Asegura la existencia del directorio padre del archivo CSV.
     * @throws IOException si ocurre un error de E/S al crear directorios
     */
    private void ensureParentDirectoryExists() throws IOException {
        Path parent = filePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
    /**
     * Crea el archivo CSV con encabezado si no existe o está vacío.
     * @return {@code true} si el archivo fue creado o reinicializado; {@code false} si ya contenía datos
     * @throws IOException si ocurre un error de E/S al escribir el archivo
     */
    private boolean createCsvFileIfMissingOrEmpty() throws IOException {
        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                writer.write(FILE_HEADER);
                writer.newLine();
            }
            return true;
        }
        return false;
    }
    /**
     * Lee todas las líneas del archivo CSV de pacientes.
     * @return lista de líneas del archivo
     * @throws IOException si ocurre un error de E/S al leer el archivo
     */
    private List<String> readCsvFileLines() throws IOException {
        return Files.readAllLines(filePath, StandardCharsets.UTF_8);
    }
    /**
     * Verifica la presencia del encabezado CSV y lo normaliza si hace falta.
     * @param lines líneas actuales del archivo CSV
     * @throws IOException si ocurre un error de E/S al actualizar el archivo
     */
    private void ensureCsvHeader(List<String> lines) throws IOException {
        if (lines == null || lines.isEmpty()) {
            List<String> normalizedLines = new ArrayList<>();
            normalizedLines.add(FILE_HEADER);
            Files.write(
                    filePath,
                    normalizedLines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return;
        }
        if (!FILE_HEADER.equals(lines.getFirst())) {
            lines.addFirst(FILE_HEADER);
            Files.write(
                    filePath,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        }
    }
    /**
     * Imprime en consola la información de inicialización del repositorio una sola vez.
     * @param fileCreated indica si el archivo fue creado durante la inicialización
     */
    private void printStartupInfoIfNeeded(boolean fileCreated) {
        if (startupInfoPrinted) {
            return;
        }
        if (fileCreated) {
            System.out.println("[CSV][PatientRepository] Archivo creado en: " + filePath);
        } else {
            System.out.println("[CSV][PatientRepository] " + filePath);
        }
        startupInfoPrinted = true;
    }
}
