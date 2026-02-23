package com.donpedromz.infraestructure.persistence.csv;

import com.donpedromz.infraestructure.integrity.IntegrityVerifier;
import com.donpedromz.infraestructure.persistence.fasta.FastaUtils;
import com.donpedromz.exceptions.CorruptedDataException;
import com.donpedromz.exceptions.PersistenceException;
import com.donpedromz.exceptions.ValidationException;
import com.donpedromz.model.Diagnostic;
import com.donpedromz.model.Patient;
import com.donpedromz.repositories.diagnostic.IDiagnosticHistoryRepository;
import com.donpedromz.repositories.diagnostic.properties.IDiagnosticStorageConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @autor juanp
 * @version 1.0
 * Implementación de {@link IDiagnosticHistoryRepository} que almacena
 * el historial de diagnósticos de un paciente en archivos CSV.
 */
public class CSVPatientDiagnosticHistoryRepository implements IDiagnosticHistoryRepository {
    /**
     * Objeto de bloqueo para sincronizar el acceso a los archivos de historial de diagnósticos.
     */
    private static final Object FILE_LOCK = new Object();
    /**
     * Nombre del directorio dentro del directorio de diagnósticos donde se almacenan las muestras de los pacientes.
     */
    private static final String SAMPLES_DIRECTORY_NAME = "samples";
    /**
     * Nombre del directorio dentro del directorio de diagnósticos
     * donde se almacenan los archivos CSV con el historial de diagnósticos de los pacientes.
     */
    private static final String HISTORY_DIRECTORY_NAME = "history";
    /**
     * Extensión de los archivos FASTA que contienen las muestras de los pacientes,
     * utilizada para identificar y validar los archivos de muestra.
     */
    private static final String FASTA_EXTENSION = ".fasta";
    /**
     * Extensión de los archivos CSV que contienen el historial de diagnósticos de los pacientes,
     */
    private static final String CSV_EXTENSION = ".csv";
    /**
     * Encabezado de las columnas del archivo CSV de historial de diagnósticos,
     * que define el formato de los datos almacenados en el archivo.
     */
    private static final String HISTORY_HEADER = "fecha_muestra,posicion_inicio_cambio,posicion_inicio_fin_cambio,tipo_cambio";
    /**
     * Ruta del directorio raíz donde se almacenan los diagnósticos de los pacientes,
     * configurada a través de la implementación de {@link IDiagnosticStorageConfig}
     * y utilizada para construir las rutas de los archivos de historial y muestras.
     */
    private final Path diagnosticsDirectory;
    /**
     * Verificador de integridad inyectado para cálculo de hashes y validación de archivos.
     */
    private final IntegrityVerifier integrityVerifier;

    /**
     * Crea una nueva instancia de CSVPatientDiagnosticHistoryRepository con la configuración de
     * almacenamiento y el verificador de integridad proporcionados.
     * @param storageConfig Configuración de almacenamiento que proporciona la ruta del directorio de diagnósticos.
     * @param integrityVerifier Verificador de integridad utilizado para calcular hashes y
     *                          validar la integridad de los archivos de muestra. No puede ser null.
     */
    public CSVPatientDiagnosticHistoryRepository(IDiagnosticStorageConfig storageConfig, IntegrityVerifier integrityVerifier) {
        if (storageConfig == null) {
            throw new ValidationException("storageConfig no puede ser null");
        }
        if (integrityVerifier == null) {
            throw new ValidationException("integrityVerifier no puede ser null");
        }
        String diagnosticsPath = storageConfig.getDiagnosticsPath();
        if (diagnosticsPath == null || diagnosticsPath.isBlank()) {
            throw new ValidationException("diagnosticsDirectory no puede ser vacío");
        }
        this.diagnosticsDirectory = Paths.get(diagnosticsPath).toAbsolutePath().normalize();
        this.integrityVerifier = integrityVerifier;
        initializeStorage();
    }

    /**
     * Inicializa el directorio raíz de diagnósticos, creándolo si no existe.
     */
    private void initializeStorage() {
        synchronized (FILE_LOCK) {
            try {
                if (!Files.exists(diagnosticsDirectory)) {
                    Files.createDirectories(diagnosticsDirectory);
                    System.out.println("[CSV][DiagnosticHistory] Directorio creado: " + diagnosticsDirectory);
                }
            } catch (IOException exception) {
                throw new PersistenceException("Error al inicializar directorio de historial de diagnósticos", exception);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param diagnostic
     * @return Cadena indicando si el historial de muestras fue actualizado
     * o una cadena vacía si no existen muestras previas para comparar.
     * En caso de error, se lanza una PersistenceException.
     */
    @Override
    public String save(Diagnostic diagnostic) {
        synchronized (FILE_LOCK) {
            validateDiagnostic(diagnostic);
            initializeStorage();
            Patient patient = diagnostic.getPatient();
            UUID patientUuid = patient.getUuid();
            Path patientDirectory = diagnosticsDirectory.resolve(patientUuid.toString());
            Path samplesDirectory = patientDirectory.resolve(SAMPLES_DIRECTORY_NAME);
            Path historyDirectory = patientDirectory.resolve(HISTORY_DIRECTORY_NAME);
            try {
                Files.createDirectories(historyDirectory);
                String diagnosticFileName = diagnostic.getSampleDate() + "_" + diagnostic.getDiagnosticUuid() + CSV_EXTENSION;
                Path historyFilePath = historyDirectory.resolve(diagnosticFileName);

                List<String> rows = new ArrayList<>();
                rows.add(HISTORY_HEADER);
                List<String[]> historyDataRows = new ArrayList<>();

                String currentSequence = diagnostic.getSampleSequence();
                String canonicalContent = ">" + patient.getPatientDocument() + "|" + diagnostic.getSampleDate()
                        + System.lineSeparator() + currentSequence;
                String currentHash = integrityVerifier.computeHash(canonicalContent);
                int previousSamplesCount = 0;

                for (Path previousSamplePath : listSampleFiles(samplesDirectory)) {
                    if (isCurrentSample(previousSamplePath, currentHash)) {
                        continue;
                    }

                    try {
                        integrityVerifier.verifyFileIntegrity(previousSamplePath, FASTA_EXTENSION);
                    } catch (CorruptedDataException corruptedException) {
                        System.out.println("[TCP][CorruptedData] " + corruptedException.getMessage());
                        continue;
                    }

                    previousSamplesCount++;

                    String previousSampleContent = readSampleContent(previousSamplePath);
                    if (previousSampleContent == null || previousSampleContent.isBlank()) {
                        continue;
                    }
                    String previousSampleDate;
                    String previousSampleSequence;
                    if (previousSampleContent.startsWith(">")) {
                        previousSampleDate = getSampleDateFromFasta(previousSampleContent);
                        previousSampleSequence = FastaUtils.getSequenceFromFasta(previousSampleContent);
                    } else {
                        previousSampleDate = "";
                        previousSampleSequence = previousSampleContent.trim().toUpperCase();
                    }
                    List<String[]> changeRows = calculateChangeRows(currentSequence, previousSampleSequence);
                    for (String[] changeRow : changeRows) {
                        historyDataRows.add(new String[]{previousSampleDate, changeRow[0], changeRow[1], changeRow[2]});
                    }
                }

                sortHistoryRowsByDate(historyDataRows);
                for (String[] dataRow : historyDataRows) {
                    rows.add(dataRow[0] + "," + dataRow[1] + "," + dataRow[2] + "," + dataRow[3]);
                }

                Files.write(
                        historyFilePath,
                        rows,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
                if (previousSamplesCount > 0) {
                    return "historial_muestras: actualizado";
                }
                return "";
            } catch (IOException exception) {
                throw new PersistenceException("Error al guardar historial de diagnóstico del paciente", exception);
            }
        }
    }

    /**
     * Lista los archivos de muestra FASTA en el directorio de muestras del paciente,
     * filtrando solo los archivos regulares que terminan con la extensión FASTA,
     * y ordenándolos alfabéticamente por nombre. Si el directorio no existe o no es un directorio,
     * se devuelve una lista vacía.
     * @param samplesDirectory Ruta del directorio de muestras del paciente donde se almacenan
     *                         los archivos FASTA de las muestras previas.
     * @return Lista de rutas de los archivos de muestra FASTA encontrados en el directorio, ordenados alfabéticamente.
     * @throws IOException Si ocurre un error al acceder al sistema de archivos para listar los archivos de muestra.
     */
    private List<Path> listSampleFiles(Path samplesDirectory) throws IOException {
        if (!Files.exists(samplesDirectory) || !Files.isDirectory(samplesDirectory)) {
            return List.of();
        }

        List<Path> sampleFiles = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(samplesDirectory)) {
            for (Path file : files) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String fileName = file.getFileName().toString().toLowerCase();
                if (!fileName.endsWith(FASTA_EXTENSION)) {
                    continue;
                }
                sampleFiles.add(file);
            }

            sortPathsByName(sampleFiles);
            return sampleFiles;
        }
    }

    /**
     * Ordena una lista de rutas de archivos alfabéticamente
     * por su nombre utilizando el algoritmo de ordenamiento por inserción.
     * @param paths Lista de rutas de archivos que se desea ordenar alfabéticamente por nombre. No debe ser null.
     */
    private void sortPathsByName(List<Path> paths) {
        for (int i = 1; i < paths.size(); i++) {
            Path current = paths.get(i);
            String currentName = current.toString();
            int j = i - 1;
            while (j >= 0 && paths.get(j).toString().compareTo(currentName) > 0) {
                paths.set(j + 1, paths.get(j));
                j--;
            }
            paths.set(j + 1, current);
        }
    }

    /**
     * Ordena una lista de filas de datos de historial de diagnósticos alfabéticamente
     * por la fecha de muestra utilizando el algoritmo de ordenamiento por inserción.
     * @param historyDataRows Lista de filas de datos de historial de diagnósticos, donde cada fila es un arreglo de cadenas
     */
    private void sortHistoryRowsByDate(List<String[]> historyDataRows) {
        for (int i = 1; i < historyDataRows.size(); i++) {
            String[] current = historyDataRows.get(i);
            LocalDate currentDate = parseHistoryDate(current[0]);
            int j = i - 1;
            while (j >= 0 && parseHistoryDate(historyDataRows.get(j)[0]).isAfter(currentDate)) {
                historyDataRows.set(j + 1, historyDataRows.get(j));
                j--;
            }
            historyDataRows.set(j + 1, current);
        }
    }

    /**
     * Verifica si el archivo de muestra representado por samplePath corresponde a la muestra actual del diagnóstico
     * comparando el hash calculado del contenido del archivo con el hash de la muestra actual.
     * Si el nombre del archivo no termina con la extensión FASTA o si el hash no coincide,
     * se considera que no es la muestra actual.
     * @param samplePath Ruta del archivo de muestra que se desea verificar
     *                   si corresponde a la muestra actual del diagnóstico. No debe ser null.
     * @param currentHash Hash de la muestra actual del diagnóstico que se
     *                    desea comparar con el hash calculado del archivo de muestra. No debe ser null ni vacío.
     * @return true si el archivo de muestra corresponde a la muestra actual del diagnóstico, false en caso contrario.
     */
    private boolean isCurrentSample(Path samplePath, String currentHash) {
        String fileName = samplePath.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(FASTA_EXTENSION)) {
            return false;
        }
        String sampleHash = fileName.substring(0, fileName.length() - FASTA_EXTENSION.length());
        return sampleHash.equals(currentHash);
    }

    /**
     * Lee el contenido de un archivo de muestra FASTA y
     * lo devuelve como una cadena. Si ocurre un error durante la lectura del archivo,
     * o si el contenido del archivo es null o está en blanco, se devuelve null.
     * @param samplePath Ruta del archivo de muestra FASTA que se desea leer. No debe ser null.
     * @return El contenido del archivo de muestra FASTA como una cadena, o null si ocurre un
     * error durante la lectura o si el contenido es null o está en blanco.
     */
    private String readSampleContent(Path samplePath) {
        try {
            String content = Files.readString(samplePath, StandardCharsets.UTF_8);
            if (content == null || content.isBlank()) {
                return null;
            }
            return content.trim();
        } catch (IOException exception) {
            return null;
        }
    }

    /**
     * Extrae la fecha de muestra de un mensaje FASTA dado. El mensaje FASTA se
     * espera que tenga un formato específico en su línea de encabezado,
     * @param fastaMessage El mensaje FASTA del cual se desea extraer la fecha de muestra. No debe ser null ni vacío.
     * @return La fecha de muestra extraída del mensaje FASTA como una cadena, o una cadena vacía si el mensaje
     * no tiene el formato esperado o si la fecha no se puede extraer.
     */
    private String getSampleDateFromFasta(String fastaMessage) {
        if (fastaMessage == null || fastaMessage.isBlank()) {
            return "";
        }
        String[] lines = fastaMessage.trim().split("\\R");
        if (lines.length < 1) {
            return "";
        }
        String headerLine = lines[0].trim();
        if (!headerLine.startsWith(">")) {
            return "";
        }
        String[] headerFields = headerLine.substring(1).split("\\|");
        if (headerFields.length < 2) {
            return "";
        }
        return headerFields[1].trim();
    }

    /**
     * Convierte una cadena de fecha de muestra del formato utilizado en los mensajes FASTA a un objeto LocalDate.
     * @param sampleDate La cadena de fecha de muestra que se desea convertir a LocalDate.
     *                   No debe ser null ni vacío, y se espera que esté en un formato de fecha válido.
     * @return Un objeto LocalDate que representa la fecha de muestra, o LocalDate.MAX si la cadena de fecha es null,
     * está en blanco o no se puede parsear correctamente.
     */
    private LocalDate parseHistoryDate(String sampleDate) {
        if (sampleDate == null || sampleDate.isBlank()) {
            return LocalDate.MAX;
        }
        try {
            return LocalDate.parse(sampleDate.trim());
        } catch (Exception exception) {
            return LocalDate.MAX;
        }
    }

    /**
     * Calcula las filas de cambios entre la secuencia actual y la secuencia previa comparando ambas secuencias
     * para identificar segmentos coincidentes y segmentos que representan cambios,
     * como reducciones o adiciones en ambos lados de la secuencia.
     * @param currentSequence La secuencia actual extraída del mensaje FASTA del diagnóstico que se está guardando.
     *                        No debe ser null ni vacío.
     * @param previousSequence La secuencia previa extraída del mensaje FASTA de una muestra anterior del paciente.
     * @return Una lista de arreglos de cadenas, donde cada arreglo representa una fila de cambio
     * con la siguiente estructura:
     * - posición de inicio del cambio en la secuencia previa (o "-1" si no aplica)
     * - posición de fin del cambio en la secuencia previa (o "-1" si no aplica)
     * - tipo de cambio (puede ser "reduccion_izquierda",
     * "reduccion_derecha", "agregado_izquierda", "agregado_derecha", "sin_cambios" o "sin_coincidencia")
     */
    private List<String[]> calculateChangeRows(String currentSequence, String previousSequence) {
        List<String[]> changeRows = new ArrayList<>();
        if (currentSequence == null || currentSequence.isBlank() || previousSequence == null || previousSequence.isBlank()) {
            changeRows.add(new String[]{"-1", "-1", "sin_coincidencia"});
            return changeRows;
        }

        int matchStartInPrevious = previousSequence.indexOf(currentSequence);
        if (matchStartInPrevious >= 0) {
            int matchEndInPrevious = matchStartInPrevious + currentSequence.length() - 1;
            addChangeSegment(changeRows, 0, matchStartInPrevious - 1, "reduccion_izquierda");
            addChangeSegment(changeRows, matchEndInPrevious + 1, previousSequence.length() - 1, "reduccion_derecha");

            if (changeRows.isEmpty()) {
                changeRows.add(new String[]{"-1", "-1", "sin_cambios"});
            }
            return changeRows;
        }

        int matchStartInCurrent = currentSequence.indexOf(previousSequence);
        if (matchStartInCurrent >= 0) {
            int matchEndInCurrent = matchStartInCurrent + previousSequence.length() - 1;
            addChangeSegment(changeRows, 0, matchStartInCurrent - 1, "agregado_izquierda");
            addChangeSegment(changeRows, matchEndInCurrent + 1, currentSequence.length() - 1, "agregado_derecha");

            if (changeRows.isEmpty()) {
                changeRows.add(new String[]{"-1", "-1", "sin_cambios"});
            }
            return changeRows;
        }

        changeRows.add(new String[]{"-1", "-1", "sin_coincidencia"});
        return changeRows;
    }

    /**
     * Agrega un segmento de cambio a la lista de filas de cambio si el rango definido por
     * start y end es válido (start <= end).
     * @param changeRows Lista de filas de cambio a la que se desea agregar el nuevo segmento de cambio.
     *                   No debe ser null.
     * @param start Posición de inicio del segmento de cambio en la secuencia (puede ser -1 si no aplica).
     * @param end Posición de fin del segmento de cambio en la secuencia (puede ser -1 si no aplica).
     * @param changeType Tipo de cambio que representa el segmento de cambio (puede ser "reduccion_izquierda",
     */
    private void addChangeSegment(List<String[]> changeRows, int start, int end, String changeType) {
        if (start > end) {
            return;
        }
        changeRows.add(new String[]{String.valueOf(start), String.valueOf(end), changeType});
    }

    /**
     * Valida que el objeto Diagnostic proporcionado no sea null y que sus campos esenciales
     * no sean null o estén en blanco. Si alguna de las validaciones falla, se lanza una ValidationException
     * con un mensaje descriptivo del error de validación.
     * @param diagnostic El objeto Diagnostic que se desea validar.
     *                   No debe ser null, y sus campos esenciales deben ser válidos.
     */
    private void validateDiagnostic(Diagnostic diagnostic) {
        if (diagnostic == null) {
            throw new ValidationException("Diagnostic no puede ser null");
        }
        if (diagnostic.getDiagnosticUuid() == null) {
            throw new ValidationException("Diagnostic uuid no puede ser null");
        }
        if (diagnostic.getSampleDate() == null || diagnostic.getSampleDate().isBlank()) {
            throw new ValidationException("Diagnostic sampleDate no puede ser vacío");
        }
        if (diagnostic.getSampleSequence() == null || diagnostic.getSampleSequence().isBlank()) {
            throw new ValidationException("Diagnostic sampleSequence no puede ser vacío");
        }
        if (diagnostic.getPatient() == null || diagnostic.getPatient().getUuid() == null) {
            throw new ValidationException("Diagnostic patient.uuid no puede ser null");
        }
    }
}
