# BioGuard TCP/IP

Sistema de diagnóstico bioinformático basado en una arquitectura **cliente-servidor TCP con cifrado SSL/TLS**. Permite registrar pacientes y enfermedades, y generar diagnósticos comparando secuencias genéticas de muestras biológicas contra enfermedades registradas.

---

## Tabla de contenidos

- [Arquitectura general](#arquitectura-general)
- [Flujo de comunicación](#flujo-de-comunicación)
- [Protocolo FASTA](#protocolo-fasta)
- [Funcionalidades principales](#funcionalidades-principales)
- [Almacenamiento de datos](#almacenamiento-de-datos)
- [Verificación de integridad](#verificación-de-integridad)
- [Configuración](#configuración)

---

## Arquitectura general

```
┌──────────────────┐        SSL/TLS (TCP)        ┌──────────────────────┐
│   bioguard_client│ ◄──────────────────────────► │   bioguard_server    │
│                  │     Puerto configurable      │                      │
│  CLI interactivo │     (por defecto 2020)        │  Procesamiento FASTA │
│  Builders FASTA  │                              │  Repositorios CSV    │
│  Scanner archivos│                              │  Integridad SHA-256  │
└──────────────────┘                              └──────────────────────┘
```

- **Cliente**: interfaz CLI que construye mensajes FASTA y los envía al servidor.
- **Servidor**: recibe mensajes TCP, identifica el tipo de operación, ejecuta la lógica de negocio y persiste los datos en archivos CSV y FASTA.

---

## Flujo de comunicación

```
Cliente                           Servidor
  │                                  │
  │  1. Construye mensaje FASTA      │
  │  2. Envía por TCP/SSL ──────────►│
  │                                  │  3. ClientHandler lee el mensaje
  │                                  │  4. FASTAProcessor itera procesadores
  │                                  │  5. El procesador compatible:
  │                                  │     - Valida formato FASTA
  │                                  │     - Ejecuta lógica de negocio
  │                                  │     - Persiste datos
  │                                  │  6. Genera respuesta TCP
  │  7. Recibe respuesta ◄──────────│
  │  8. Muestra resultado al usuario │
  │                                  │
```

El **FASTAProcessor** actúa como cadena de responsabilidad: prueba cada procesador (`FASTADiagnose`, `FASTAPatientRegister`, `FASTADiseaseRegister`) en orden. El primero que no lance `InvalidMessageFormatException` procesa el mensaje. Si ninguno lo acepta, responde con un error de formato.

---

## Protocolo FASTA

Tres formatos de mensaje, diferenciados por cantidad de líneas y campos del encabezado:

### Registro de paciente (1 línea, 8 campos)

```
>Documento|Nombre|Apellido|Edad|Email|Género|Ciudad|País
```

**Ejemplo:**
```
>12345678|Juan|Perez|30|juan@mail.com|MASCULINO|Bogota|Colombia
```

### Registro de enfermedad (2 líneas, 2 campos)

```
>NombreEnfermedad|NivelInfectividad
SECUENCIA_GENETICA
```

**Ejemplo:**
```
>ebola|ALTA
GAGTATGTGAATAGATATATATTAGTAGTAGTAAAGTTTTAAA
```

Niveles de infectividad: `ALTA`, `MEDIA`, `BAJA`

### Diagnóstico (2 líneas, 2 campos — documento + fecha)

```
>DocumentoPaciente|YYYY-MM-DD
SECUENCIA_GENETICA_MUESTRA
```

**Ejemplo:**
```
>12213021|2025-02-19
GTGATGATTAGTGGTAGTAGTAGGGATAGT
```

---

## Funcionalidades principales

### 1. Registro de pacientes

- Valida formato de documento (numérico), nombre/apellido (alfabético), email, género, ciudad, país y edad (1–120).
- Detecta documentos duplicados y rechaza el registro con `ConflictException`.
- Persiste en archivo CSV con encabezado estandarizado de 9 columnas (UUID, documento, nombre, apellido, edad, email, género, ciudad, país).

### 2. Registro de enfermedades

- Valida nombre, nivel de infectividad (`ALTA`, `MEDIA`, `BAJA`) y secuencia genética (solo caracteres A, C, G, T).
- Almacena cada enfermedad como un archivo `.fasta` individual.
- El nombre del archivo es el hash SHA-256 del contenido, garantizando detección de duplicados y verificación de integridad.

### 3. Generación de diagnósticos

Flujo completo al recibir un mensaje de diagnóstico:

1. **Parseo**: extrae documento del paciente, fecha de muestra y secuencia genética.
2. **Búsqueda de paciente**: verifica que el paciente exista por su documento.
3. **Detección de duplicados**: verifica que la misma secuencia no haya sido procesada previamente para ese paciente.
4. **Comparación genética**: busca coincidencias exactas de la secuencia de la muestra dentro de las secuencias de todas las enfermedades registradas.
5. **Persistencia** (si hay coincidencias):
   - **Diagnóstico CSV**: genera un archivo CSV por diagnóstico con UUID, fecha, enfermedades detectadas y posiciones de la secuencia.
   - **Archivo de muestra**: almacena la secuencia con nombre = hash SHA-256 para deduplicación.
   - **Historial de mutaciones**: compara la secuencia actual contra muestras anteriores del mismo paciente, detectando reducciones y adiciones.
   - **Reporte de alta infectividad**: si el paciente acumula enfermedades de nivel `ALTA`, se registra en un reporte consolidado.

### 4. Reportes de alta infectividad

- Genera y mantiene un CSV consolidado con pacientes que presentan enfermedades de alta infectividad.
- Incluye documento del paciente y las enfermedades de nivel `ALTA` detectadas.

### 5. Historial de mutaciones

- Compara cada nueva muestra del paciente contra sus muestras previas almacenadas.
- Identifica cambios en la secuencia genética: `reduccion_izquierda`, `reduccion_derecha`, `agregado_izquierda`, `agregado_derecha`, `sin_cambios` o `sin_coincidencia`.
- Genera un archivo CSV de historial por cada diagnóstico.

---

## Almacenamiento de datos

| Dato | Formato | Ubicación |
|---|---|---|
| Pacientes | CSV único | `data/patients.csv` |
| Enfermedades | Archivos `.fasta` individuales (nombre = SHA-256) | `data/diseases/` |
| Diagnósticos | CSV por diagnóstico, dentro de carpeta por paciente | `data/diagnostics/{patientUUID}/generated_diagnostics/` |
| Muestras | Archivos `.fasta` (nombre = SHA-256 de la secuencia) | `data/diagnostics/{patientUUID}/samples/` |
| Historial | CSV por diagnóstico | `data/diagnostics/{patientUUID}/history/` |
| Reporte alta infectividad | CSV consolidado | `data/reports/high_infectiousness/` |

---

## Verificación de integridad

El sistema utiliza **SHA-256** para garantizar la integridad de los datos almacenados:

- Los archivos de enfermedades y muestras se nombran con el hash SHA-256 de su contenido.
- Al leer un archivo, se recalcula el hash del contenido y se compara con el nombre del archivo.
- Si no coinciden, se lanza una `CorruptedDataException` y el dato corrupto se omite del procesamiento.

---

## Configuración

### Servidor (`bioguard_server/src/main/resources/application.properties`)

```properties
server.port=2020
ssl.keystore.path=bioguard_keystore.p12
ssl.keystore.password=<contraseña>
storage.csv.patients.path=data/patients.csv
storage.diseases.directory=data/diseases
storage.diagnostics.directory=data/diagnostics
storage.reports.high_infectiousness.directory=data/reports/high_infectiousness/
```

### Cliente (`bioguard_client/src/main/resources/application.properties`)

```properties
server.host=localhost
server.port=2020
ssl.truststore.path=bioguard_keystore.p12
ssl.truststore.password=<contraseña>
```

---
