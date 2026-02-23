package com.donpedromz.infrastructure.persistence.csv;

import com.donpedromz.infrastructure.integrity.IntegrityVerifier;
import com.donpedromz.exceptions.ConflictException;
import com.donpedromz.exceptions.CorruptedDataException;
import com.donpedromz.exceptions.PersistenceException;
import com.donpedromz.exceptions.ValidationException;
import com.donpedromz.model.Diagnostic;
import com.donpedromz.model.Disease;
import com.donpedromz.repositories.diagnostic.IDiagnosticRepository;
import com.donpedromz.repositories.diagnostic.properties.IDiagnosticStorageConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author juanp
 * @version 1.0
 * Implementación de {@link IDiagnosticRepository} que persiste diagnósticos en archivos CSV organizados por paciente.
 * Cada diagnóstico se guarda en un archivo CSV separado dentro de un subdirectorio específico para el paciente,
 * y las muestras FASTA originales se almacenan en un subdirectorio de
 * muestras para evitar duplicados mediante el uso de hashes SHA-256.
 */
public class CSVDiagnosticRepository implements IDiagnosticRepository {
    /**
     * Objeto de bloqueo global para sincronizar operaciones de lectura y escritura en el sistema de archivos.
     */
    private static final Object GLOBAL_LOCK = new Object();
    /**
     * Encabezado estándar para los archivos CSV de diagnóstico, que define las columnas:
     */
    private static final String DIAGNOSTIC_FILE_HEADER =
            "uuid_diagnostico,fecha,uuid_virus,virus,posicion_inicio,posicion_fin";
    /**
     * Nombres de los subdirectorios utilizados para organizar los
     * archivos de diagnóstico y muestras FASTA dentro del directorio raíz de diagnósticos.
     */
    private static final String GENERATED_DIAGNOSTICS_DIRECTORY_NAME = "generated_diagnostics";
    /**
     * Subdirectorio donde se almacenan las muestras FASTA originales de los pacientes,
     * utilizando hashes SHA-256 en los nombres de archivo para
     * evitar duplicados y facilitar la verificación de integridad.
     */
    private static final String SAMPLES_DIRECTORY_NAME = "samples";
    /**
     * Extensiones de archivo utilizadas para identificar los
     * archivos de muestra FASTA y los archivos de diagnóstico CSV.
     */
    private static final String FASTA_EXTENSION = ".fasta";
    /**
     * Extensión de archivo utilizada para identificar los archivos de diagnóstico CSV generados,
     */
    private static final String CSV_EXTENSION = ".csv";
    /**
     * Valida que la fecha siga el formato ISO YYYY-MM-DD.
     */
    private static final String DATE_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
    /**
     * Valida secuencias genéticas compuestas exclusivamente por A, C, G y T.
     */
    private static final String SEQUENCE_REGEX = "^[ACGT]+$";
    /**
     * Valida documentos numéricos.
     */
    private static final String DOCUMENT_REGEX = "^\\d+$";
    /**
     * Longitud mínima requerida para la secuencia genética extraída de la muestra FASTA,
     */
    private static final int MIN_DIAGNOSE_SEQUENCE_LENGTH = 7;
    /**
     * Ruta del directorio raíz donde se almacenan los archivos de diagnóstico y muestras FASTA,
     */
    private final Path diagnosticsDirectory;
    /**
     * Verificador de integridad inyectado para cálculo de hashes y validación de archivos.
     */
    private final IntegrityVerifier integrityVerifier;
    /**
     * Crea el repositorio de diagnósticos utilizando la configuración proporcionada.
     * @param storageConfig configuración que proporciona la ruta del directorio de diagnósticos
     * @param integrityVerifier verificador de integridad para hashing y validación de archivos
     */
    public CSVDiagnosticRepository(IDiagnosticStorageConfig storageConfig, IntegrityVerifier integrityVerifier) {
        if (storageConfig == null) {
            throw new ValidationException("storageConfig no puede ser null");
        }
        String diagnosticsDirectoryPath = storageConfig.getDiagnosticsPath();
        if (diagnosticsDirectoryPath == null || diagnosticsDirectoryPath.isBlank()) {
            throw new ValidationException("La ruta del directorio de diagnósticos es obligatoria.");
        }
        if (integrityVerifier == null) {
            throw new ValidationException("integrityVerifier no puede ser null");
        }
        this.diagnosticsDirectory = Paths.get(diagnosticsDirectoryPath).toAbsolutePath().normalize();
        this.integrityVerifier = integrityVerifier;
        initializeStorage();
    }
    /**
     * Guarda un diagnóstico en un archivo CSV dentro de un subdirectorio específico para el paciente,
     * y almacena la secuencia genética de la muestra
     * en un subdirectorio de muestras utilizando un hash SHA-256 para evitar duplicados.
     * @param entity diagnóstico a guardar
     */
    @Override
    public String save(Diagnostic entity) {
        synchronized (GLOBAL_LOCK) {
            validateForSave(entity);
            try {
                initializeStorage();
                if (entity.getDiagnosticUuid() == null) {
                    entity.setDiagnosticUuid(UUID.randomUUID());
                }
                Path patientDirectory = diagnosticsDirectory.resolve(entity.getPatient().getUuid().toString());
                Path samplesDirectory = patientDirectory.resolve(SAMPLES_DIRECTORY_NAME);
                Path generatedDiagnosticsDirectory = patientDirectory.resolve(GENERATED_DIAGNOSTICS_DIRECTORY_NAME);
                Files.createDirectories(samplesDirectory);
                Files.createDirectories(generatedDiagnosticsDirectory);

                String sampleSequence = entity.getSampleSequence();
                String canonicalContent = buildCanonicalSampleContent(
                        entity.getPatient().getPatientDocument(),
                        entity.getSampleDate(),
                        sampleSequence);
                String sampleHash = integrityVerifier.computeHash(canonicalContent);
                Path samplePath = samplesDirectory.resolve(sampleHash + FASTA_EXTENSION);
                if (Files.exists(samplePath)) {
                    throw new ConflictException("ya existe un diagnostico registrado para este paciente con la misma muestra y fecha");
                }
                Files.writeString(
                        samplePath,
                        canonicalContent,
                        StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
                );

                String diagnosticFileName = entity.getSampleDate() + "_" + entity.getDiagnosticUuid() + CSV_EXTENSION;
                Path diagnosticFilePath = generatedDiagnosticsDirectory.resolve(diagnosticFileName);
                String patientSequence = entity.getSampleSequence();
                List<String> csvRows = getStrings(entity, patientSequence);
                Files.write(
                        diagnosticFilePath,
                        csvRows,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
                return "enfermedades_detectadas: " + entity.getDiseases().size();
            } catch (IOException exception) {
                throw new PersistenceException("Error al guardar diagnóstico", exception);
            }
        }
    }

    /**
     * Genera las filas de texto que representan el contenido del archivo CSV para un diagnóstico dado,
     * incluyendo el encabezado y una fila por cada enfermedad detectada,
     * con las posiciones de la secuencia genética del paciente dentro de la secuencia de la enfermedad.
     * @param entity diagnóstico para el cual se generarán las filas del CSV
     * @param patientSequence secuencia genética extraída de la muestra FASTA del paciente, que se buscará dentro
     *                        de las secuencias de las enfermedades para determinar las posiciones de inicio y fin
     * @return una lista de cadenas de texto que representan las filas del archivo CSV,
     * incluyendo el encabezado y las filas de datos para cada enfermedad detectada.
     */
    private static List<String> getStrings(Diagnostic entity, String patientSequence) {
        List<String> csvRows = new ArrayList<>();
        csvRows.add(DIAGNOSTIC_FILE_HEADER);
        for (Disease disease : entity.getDiseases()) {
            if (disease == null || disease.getUuid() == null || disease.getDiseaseName() == null || disease.getGeneticSequence() == null) {
                continue;
            }
            String diseaseSequence = disease.getGeneticSequence().toUpperCase();
            int start = diseaseSequence.indexOf(patientSequence);
            int end = start < 0 ? -1 : start + patientSequence.length() - 1;
            csvRows.add(entity.getDiagnosticUuid() + "," + entity.getSampleDate() + "," + disease.getUuid() + "," + disease.getDiseaseName() + "," + start + "," + end);
        }
        return csvRows;
    }
    /**
     * Verifica si ya existe una muestra registrada para el paciente con la misma secuencia y fecha.
     * Una muestra se considera duplicada solo si tanto la secuencia genética como la fecha coinciden.
     * @param patientUuid UUID del paciente
     * @param sampleSequence secuencia genética de la muestra
     * @param sampleDate fecha de la muestra en formato YYYY-MM-DD
     * @param patientDocument documento del paciente
     * @return {@code true} si ya existe una muestra con la misma secuencia y fecha para ese paciente
     */
    @Override
    public boolean existsByPatientAndSample(UUID patientUuid, String sampleSequence, String sampleDate, String patientDocument) {
        synchronized (GLOBAL_LOCK) {
            if (patientUuid == null || sampleSequence == null || sampleSequence.isBlank()
                    || sampleDate == null || sampleDate.isBlank()
                    || patientDocument == null || patientDocument.isBlank()) {
                return false;
            }

            Path samplesDirectory = diagnosticsDirectory
                    .resolve(patientUuid.toString())
                    .resolve(SAMPLES_DIRECTORY_NAME);

            if (!Files.exists(samplesDirectory) || !Files.isDirectory(samplesDirectory)) {
                return false;
            }

            String canonicalContent = buildCanonicalSampleContent(patientDocument, sampleDate, sampleSequence);
            String sampleHash = integrityVerifier.computeHash(canonicalContent);
            Path samplePath = samplesDirectory.resolve(sampleHash + FASTA_EXTENSION);

            if (Files.exists(samplePath) && Files.isRegularFile(samplePath)) {
                try {
                    integrityVerifier.verifyFileIntegrity(samplePath, FASTA_EXTENSION);
                    return true;
                } catch (CorruptedDataException corruptedException) {
                    System.out.println("[TCP][CorruptedData] " + corruptedException.getMessage());
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * Construye el contenido canónico de una muestra FASTA para hashing y almacenamiento.
     * El formato incluye la fecha para que muestras con diferente fecha generen hashes distintos.
     * @param patientDocument documento del paciente
     * @param sampleDate fecha de la muestra
     * @param sampleSequence secuencia genética de la muestra
     * @return contenido canónico en formato FASTA: {@code >documento|fecha\nSECUENCIA}
     */
    private String buildCanonicalSampleContent(String patientDocument, String sampleDate, String sampleSequence) {
        return ">" + patientDocument + "|" + sampleDate + System.lineSeparator() + sampleSequence;
    }

    /**
     * Inicializa el almacenamiento de diagnósticos creando el directorio raíz si no existe.
     */
    private void initializeStorage() {
        synchronized (GLOBAL_LOCK) {
            try {
                Files.createDirectories(diagnosticsDirectory);
            } catch (IOException exception) {
                throw new PersistenceException("Error al inicializar almacenamiento de diagnósticos", exception);
            }
        }
    }
    private void validateForSave(Diagnostic diagnostic) {
        if (diagnostic == null) {
            throw new ValidationException("Diagnostic no puede ser null");
        }

        List<String> invalidFields = new ArrayList<>();

        if (diagnostic.getPatient() == null) {
            invalidFields.add("patient");
        } else {
            if (diagnostic.getPatient().getUuid() == null) {
                invalidFields.add("patient.uuid");
            }
            String patientDocument = diagnostic.getPatient().getPatientDocument();
            if (patientDocument == null || patientDocument.isBlank() || !patientDocument.trim().matches(DOCUMENT_REGEX)) {
                invalidFields.add("patient.document");
            }
        }

        String sampleDate = diagnostic.getSampleDate();
        if (sampleDate == null || sampleDate.isBlank() || !sampleDate.trim().matches(DATE_REGEX)) {
            invalidFields.add("sampleDate");
        } else {
            try {
                LocalDate.parse(sampleDate.trim());
            } catch (RuntimeException exception) {
                invalidFields.add("sampleDate");
            }
        }

        String sampleSequence = diagnostic.getSampleSequence();
        if (sampleSequence == null || sampleSequence.isBlank()) {
            invalidFields.add("sampleSequence");
        } else {
            if (!sampleSequence.matches(SEQUENCE_REGEX)) {
                invalidFields.add("sampleSequence");
            } else if (sampleSequence.length() < MIN_DIAGNOSE_SEQUENCE_LENGTH) {
                invalidFields.add("sampleSequence (mínimo " + MIN_DIAGNOSE_SEQUENCE_LENGTH + " nucleótidos)");
            }
        }
        if (diagnostic.getDiseases() == null || diagnostic.getDiseases().isEmpty()) {
            invalidFields.add("diseases");
        }
        if (!invalidFields.isEmpty()) {
            throw new ValidationException("Campos inválidos: " + String.join(", ", invalidFields));
        }
    }

}
