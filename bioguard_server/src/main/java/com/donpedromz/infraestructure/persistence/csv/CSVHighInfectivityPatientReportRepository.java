package com.donpedromz.infraestructure.persistence.csv;

import com.donpedromz.exceptions.PersistenceException;
import com.donpedromz.exceptions.ValidationException;
import com.donpedromz.model.Diagnostic;
import com.donpedromz.model.Disease;
import com.donpedromz.model.InfectiousnessLevel;
import com.donpedromz.model.Patient;
import com.donpedromz.repositories.diagnostic.properties.IDiagnosticStorageConfig;
import com.donpedromz.repositories.diagnostic.IHighInfectivityPatientReportRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @author juanp
 * Implementación de {@link IHighInfectivityPatientReportRepository}
 * que almacena reportes de pacientes con alta infecciosidad en un archivo CSV.
 */
public class CSVHighInfectivityPatientReportRepository implements IHighInfectivityPatientReportRepository {
    /**
     * Objeto de bloqueo para sincronizar el acceso al archivo de reporte y evitar condiciones de carrera.
     */
    private static final Object FILE_LOCK = new Object();
    /**
     * Nombre del archivo CSV donde se almacenarán los reportes de pacientes con alta infecciosidad.
     */
    private static final String REPORT_FILE_NAME = "high_infectivity_patients_report.csv";
    /**
     * Número mínimo de virus de alta infecciosidad detectados en un diagnóstico
     * para que un paciente cumpla el criterio de reporte.
     */
    private static final int HIGH_INFECTIVITY_THRESHOLD = 3;
    /**
     * Encabezado del archivo CSV que define las columnas para el reporte de pacientes con alta infecciosidad.
     */
    private static final String REPORT_HEADER =
            "documento,total_virus_detectados,cantiad_virus_altamente_infecciosos,lista_virus_contagio_normal_o_medio,lista_virus_altmanete_infecciosos";
    /**
     * Ruta absoluta y normalizada del archivo CSV donde se almacenarán los reportes de pacientes con alta infecciosidad.
     */
    private final Path reportFilePath;
    /**
     * Construye el repositorio de reporte a partir de la configuración de diagnósticos.
     *
     * @param storageConfig configuración de almacenamiento para obtener el directorio base
     */
    public CSVHighInfectivityPatientReportRepository(IDiagnosticStorageConfig storageConfig) {
        if (storageConfig == null) {
            throw new ValidationException("storageConfig no puede ser null");
        }
        String reportsDirectory = storageConfig.getHighInfectiousnessReportsPath();
        if (reportsDirectory == null || reportsDirectory.isBlank()) {
            throw new ValidationException("reportsDirectory no puede ser vacío");
        }
        this.reportFilePath = Path.of(reportsDirectory).toAbsolutePath().normalize().resolve(REPORT_FILE_NAME);
        try {
            initializeReportFile();
        } catch (IOException exception) {
            throw new PersistenceException("Error al inicializar reporte de alta infecciosidad", exception);
        }
    }
    /**
     * Evalúa un diagnóstico y lo registra en el reporte CSV si cumple criterio de alta infecciosidad.
     * @param diagnostic diagnóstico a evaluar para el reporte
     */
    @Override
    public String save(Diagnostic diagnostic) {
        synchronized (FILE_LOCK) {
            if (diagnostic == null) {
                throw new ValidationException("Diagnostic no puede ser null");
            }
            Patient patient = diagnostic.getPatient();
            if (patient == null || patient.getPatientDocument() == null || patient.getPatientDocument().isBlank()) {
                throw new ValidationException("Diagnostic patient.document no puede ser vacío");
            }
            List<Disease> diseases = diagnostic.getDiseases();
            if (diseases == null || diseases.isEmpty()) {
                return "";
            }
            List<Disease> validDiseases = new ArrayList<>();
            for (Disease disease : diseases) {
                if (disease == null) {
                    continue;
                }
                if (disease.getDiseaseName() == null || disease.getDiseaseName().isBlank()) {
                    continue;
                }
                validDiseases.add(disease);
            }
            if (validDiseases.isEmpty()) {
                return "";
            }
            List<String> highInfectivityVirusNames = new ArrayList<>();
            List<String> normalOrMediumVirusNames = new ArrayList<>();
            for (Disease disease : validDiseases) {
                String diseaseName = disease.getDiseaseName().trim();
                if (isHighInfectiousness(disease.getInfectiousnessLevel())) {
                    highInfectivityVirusNames.add(diseaseName);
                } else {
                    normalOrMediumVirusNames.add(diseaseName);
                }
            }

            if (highInfectivityVirusNames.size() < HIGH_INFECTIVITY_THRESHOLD) {
                return "";
            }

            System.out.println(
                    "[REPORT][HighInfectivity] Paciente " + patient.getPatientDocument().trim()
                            + " cumple criterio: " + highInfectivityVirusNames.size()
                            + " virus ALTA de " + validDiseases.size() + " detectados"
            );

            try {
                ensureReportStorageAvailableForSave();
                String csvRow = buildCsvRow(
                        patient.getPatientDocument().trim(),
                        validDiseases.size(),
                        highInfectivityVirusNames.size(),
                        String.join("|", normalOrMediumVirusNames),
                        String.join("|", highInfectivityVirusNames)
                );

                Files.writeString(
                        reportFilePath,
                        csvRow + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
                return "criterio_alta_infecciosidad: cumple (>= " + HIGH_INFECTIVITY_THRESHOLD + ")";
                } catch (IOException exception) {
                    throw new PersistenceException("Error al guardar reporte de alta infecciosidad", exception);
                }
            }
        }
    
        /**
         * Garantiza que el almacenamiento del reporte exista antes de guardar.
         * Si el directorio o el archivo no existen, se crean. Si el archivo existe pero está vacío,
         * se escribe el encabezado.
         * @throws IOException si ocurre un error de E/S al verificar o crear recursos
         */
        private void ensureReportStorageAvailableForSave() throws IOException {
            Path parent = reportFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                System.out.println("[REPORT][HighInfectivity] Directorio de reportes recreado: " + parent);
            }
            if (!Files.exists(reportFilePath)) {
                initializeReportFile();
                return;
            }
            if (Files.size(reportFilePath) == 0L) {
                Files.writeString(
                        reportFilePath,
                        REPORT_HEADER + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
                System.out.println("[REPORT][HighInfectivity] Encabezado de reporte restaurado: " + reportFilePath);
            }
    }
    /**
     * Inicializa el archivo de reporte, creando directorios y encabezado cuando corresponda.
     * @throws IOException si ocurre un error de E/S al preparar el archivo
     */
    private void initializeReportFile() throws IOException {
        Path parent = reportFilePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(reportFilePath) || Files.size(reportFilePath) == 0L) {
            Files.writeString(
                    reportFilePath,
                    REPORT_HEADER + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            System.out.println("[REPORT][HighInfectivity] Archivo CSV creado: " + reportFilePath);
        } else {
            System.out.println("[REPORT][HighInfectivity] Archivo CSV existente: " + reportFilePath);
        }
    }
    /**
     * Construye la fila CSV con los datos del reporte de alta infecciosidad.
     * @param patientDocument documento del paciente
     * @param totalVirusesDetected total de virus detectados en el diagnóstico
     * @param highInfectivityVirusesCount cantidad de virus de alta infecciosidad
     * @param normalOrMediumVirusList lista de virus de contagio normal o medio
     * @param highInfectivityVirusList lista de virus altamente infecciosos
     * @return fila serializada en formato CSV
     */
    private String buildCsvRow(
            String patientDocument,
            int totalVirusesDetected,
            int highInfectivityVirusesCount,
            String normalOrMediumVirusList,
            String highInfectivityVirusList
    ) {
        return String.join(",",
                escapeCsv(patientDocument),
                escapeCsv(totalVirusesDetected),
                escapeCsv(highInfectivityVirusesCount),
                escapeCsv(normalOrMediumVirusList),
                escapeCsv(highInfectivityVirusList)
        );
    }
    /**
     * Escapa valores para escritura segura en formato CSV.
     * @param value valor a serializar
     * @return texto escapado según reglas básicas CSV
     */
    private String escapeCsv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        boolean shouldQuote = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        String escaped = text.replace("\"", "\"\"");
        return shouldQuote ? "\"" + escaped + "\"" : escaped;
    }
    /**
     * Indica si un nivel de infecciosidad corresponde a categoría alta.
     * @param infectiousnessLevel nivel de infecciosidad
     * @return {@code true} si el valor equivale a {@link InfectiousnessLevel#ALTA}
     */
    private boolean isHighInfectiousness(InfectiousnessLevel infectiousnessLevel) {
        return infectiousnessLevel == InfectiousnessLevel.ALTA;
    }
}
