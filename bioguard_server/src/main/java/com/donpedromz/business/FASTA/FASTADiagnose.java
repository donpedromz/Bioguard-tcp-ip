package com.donpedromz.business.FASTA;

import com.donpedromz.business.exceptions.DataValidationException;
import com.donpedromz.business.IMessageProcessor;
import com.donpedromz.business.FASTA.exceptions.InvalidMessageFormatException;
import com.donpedromz.business.FASTA.exceptions.InvalidFastaFormatException;
import com.donpedromz.business.FASTA.dto.DiagnoseMessageDto;
import com.donpedromz.data.diagnostic.IDiagnosticHistoryRepository;
import com.donpedromz.data.diagnostic.IHighInfectivityPatientReportRepository;
import com.donpedromz.data.diagnostic.IDiagnosticRepository;
import com.donpedromz.data.disease.IDiseaseRepository;
import com.donpedromz.data.exceptions.CorruptedDataException;
import com.donpedromz.data.exceptions.NotFoundException;
import com.donpedromz.data.patient.IPatientRepository;
import com.donpedromz.entities.Diagnostic;
import com.donpedromz.entities.Disease;
import com.donpedromz.entities.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @version 1.0
 * @author juanp
 * Procesador de mensajes FASTA para generar diagnósticos a partir de muestras genéticas de pacientes.
 */
public class FASTADiagnose implements IMessageProcessor {
    /**
     * Constantes para validación de formato FASTA específico de diagnóstico:
     * - Se esperan exactamente 2 líneas: una de encabezado y una de secuencia genética:
     * >documento|fecha
     * SECUENCIA_GENÉTICA
     */
    private static final int EXPECTED_LINES = 2;
    /**
     * El encabezado debe contener exactamente 2 campos:
     * >documento|fecha
     */
    private static final int EXPECTED_HEADER_FIELDS = 2;
    /**
     * La fecha en el encabezado debe seguir el formato YYYY-MM-DD, validado con esta expresión regular.
     */
    private static final String DATE_FORMAT_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
    /**
     * Mensaje de error para muestras duplicadas, indicando que no se puede generar un diagnóstico
     * porque la muestra ya fue registrada previamente para el paciente.
     * Este mensaje se devuelve cuando se detecta que el mismo mensaje FASTA ya ha sido procesado
     * para el mismo paciente, evitando diagnósticos redundantes.
     */
    private static final String DUPLICATE_SAMPLE_MESSAGE = "[TCP] no se puede generar diagnóstico: la muestra ya fue registrada previamente para este paciente";
    /**
     * Mensaje de éxito para diagnóstico generado, indicando que el proceso se completó correctamente.
     */
    private static final String SUCCESS_MESSAGE = "[TCP] diagnostico generado exitosamente";
    /**
     * Separador utilizado para concatenar mensajes de operación adicionales al mensaje de éxito principal.
     */
    private static final String MESSAGE_SEPARATOR = " | ";
    /**
     * Repositorio de pacientes para acceder a la información de los pacientes registrados,
     * buscar por documento y obtener sus UUIDs.
     */
    private final IPatientRepository patientRepository;
    /**
     * Repositorio de enfermedades para acceder a las enfermedades registradas, obtener sus secuencias genéticas
     * y realizar las comparaciones necesarias para generar el diagnóstico a partir de la muestra del paciente.
     */
    private final IDiseaseRepository diseaseRepository;
    /**
     * Repositorio de diagnósticos para guardar los diagnósticos generados a partir de las muestras de los pacientes,
     * así como para verificar si una muestra ya ha sido registrada previamente para un paciente específico,
     * evitando diagnósticos duplicados.
     */
    private final IDiagnosticRepository diagnosticRepository;
    /**
     * Repositorio para generar reportes de pacientes con alta infectividad.
     */
    private final IHighInfectivityPatientReportRepository highInfectivityPatientReportRepository;
    /**
     * Repositorio para almacenar el historial de mutaciones del paciente.
     */
    private final IDiagnosticHistoryRepository diagnosticHistoryRepository;
    /**
     * Construye el procesador de diagnóstico FASTA.
     * @param patientRepository repositorio de pacientes
     * @param diseaseRepository repositorio de enfermedades
     * @param diagnosticRepository repositorio de diagnósticos
     * @param highInfectivityPatientReportRepository repositorio de reporte para pacientes con más de 3 virus ALTA
         * @param diagnosticHistoryRepository repositorio de historial de diagnósticos por paciente
     */
    public FASTADiagnose(
            IPatientRepository patientRepository,
            IDiseaseRepository diseaseRepository,
            IDiagnosticRepository diagnosticRepository,
             IHighInfectivityPatientReportRepository highInfectivityPatientReportRepository,
             IDiagnosticHistoryRepository diagnosticHistoryRepository) {
        if (patientRepository == null) {
            throw new IllegalArgumentException("patientRepository no puede ser null");
        }
        if (diseaseRepository == null) {
            throw new IllegalArgumentException("diseaseRepository no puede ser null");
        }
        if (diagnosticRepository == null) {
            throw new IllegalArgumentException("diagnosticRepository no puede ser null");
        }
        if (highInfectivityPatientReportRepository == null) {
            throw new IllegalArgumentException("highInfectivityPatientReportRepository no puede ser null");
        }
        if (diagnosticHistoryRepository == null) {
            throw new IllegalArgumentException("diagnosticHistoryRepository no puede ser null");
        }
        this.patientRepository = patientRepository;
        this.diseaseRepository = diseaseRepository;
        this.diagnosticRepository = diagnosticRepository;
        this.highInfectivityPatientReportRepository = highInfectivityPatientReportRepository;
        this.diagnosticHistoryRepository = diagnosticHistoryRepository;
    }

    /**
     * Valida y normaliza el mensaje FASTA de diagnóstico.
     * @param message mensaje FASTA bruto recibido por TCP
     * @return DTO con documento, fecha, secuencia y mensaje original normalizado
     */
    private DiagnoseMessageDto verify(String message) {
        if (message == null || message.isBlank()) {
            throw new InvalidFastaFormatException("FASTA message for Diagnose cannot be empty");
        }
        if (!message.startsWith(">")) {
            throw new InvalidFastaFormatException("Invalid FASTA format for Diagnose");
        }
        String[] lines = message.trim().split("\\R");
        if (lines.length != EXPECTED_LINES) {
            throw new InvalidFastaFormatException(
                    "FASTA format for Diagnose should have exactly " + EXPECTED_LINES + " lines");
        }
        String[] header = lines[0].split("\\|");
        if (header.length != EXPECTED_HEADER_FIELDS) {
            throw new InvalidFastaFormatException("FASTA header for Diagnose should have exactly "
                    + EXPECTED_HEADER_FIELDS + " fields separated by '|'");
        }
        String patientDocument = header[0].substring(1).trim();
        String sampleDate = header[1].trim();
        if (!sampleDate.matches(DATE_FORMAT_REGEX)) {
            throw new InvalidFastaFormatException(
                    "FASTA header for Diagnose requires a date in YYYY-MM-DD format as second field");
        }
        String geneticSequence = lines[1].trim().toUpperCase(Locale.ROOT);

        return new DiagnoseMessageDto(
            patientDocument,
            sampleDate,
            geneticSequence
        );
    }

    /**
     * Busca coincidencias exactas de la secuencia de paciente en todas las enfermedades.
     * @param patientSequence secuencia genética de la muestra del paciente
     * @param diseases enfermedades registradas
     * @return lista de enfermedades detectadas cuya secuencia contiene la del paciente
     */
    private List<Disease> findMatches(String patientSequence, List<Disease> diseases) {
        List<Disease> matches = new ArrayList<>();
        for (Disease disease : diseases) {
            Disease match = findExactMatch(patientSequence, disease);
            if (match != null) {
                matches.add(match);
            }
        }
        return matches;
    }

    /**
     * Busca una coincidencia exacta de la secuencia del paciente dentro de una enfermedad.
     * @param patientSequence secuencia de la muestra del paciente
     * @param disease enfermedad candidata
     * @return la enfermedad con su secuencia normalizada si coincide, o {@code null} si no coincide
     */
    private Disease findExactMatch(String patientSequence, Disease disease) {
        if (disease == null || disease.getGeneticSequence() == null || disease.getGeneticSequence().isBlank()) {
            return null;
        }

        String diseaseSequence = disease.getGeneticSequence().toUpperCase(Locale.ROOT);
        int start = diseaseSequence.indexOf(patientSequence);
        if (start < 0) {
            return null;
        }

        return new Disease(
                disease.getUuid(),
                disease.getDiseaseName(),
                disease.getInfectiousnessLevel(),
            diseaseSequence);
    }

    /**
     * Procesa el mensaje FASTA y genera un diagnóstico persistido cuando aplica.
     * @param message mensaje FASTA de entrada
     * @return respuesta TCP de resultado o causa de rechazo
     */
    @Override
    public String process(String message) {
        try {
            DiagnoseMessageDto diagnoseMessage = verify(message);
            if (diagnoseMessage.patientDocument() == null || diagnoseMessage.sampleDate() == null
                    || diagnoseMessage.geneticSequence() == null) {
                throw new InvalidFastaFormatException("Diagnose FASTA contains null values");
            }
            Patient patient = patientRepository.getByDocument(diagnoseMessage.patientDocument());
            if (patient == null) {
                throw new NotFoundException("no se encontró ningún paciente con dicho documento");
            }
            if (patient.getUuid() == null) {
                throw new NotFoundException("no se encontró UUID para el paciente del documento enviado");
            }
            if (diagnosticRepository.existsByPatientAndSample(patient.getUuid(), diagnoseMessage.geneticSequence())) {
                return DUPLICATE_SAMPLE_MESSAGE;
            }
            List<Disease> diseases = diseaseRepository.findAll();
            List<Disease> diagnosticDiseases = findMatches(diagnoseMessage.geneticSequence(), diseases);
            if (diagnosticDiseases.isEmpty()) {
                throw new NotFoundException("no se encontró ninguna enfermedad que coincida con dicha secuencia");
            }

            Diagnostic diagnostic = new Diagnostic(
                    UUID.randomUUID(),
                    diagnoseMessage.sampleDate(),
                    diagnoseMessage.geneticSequence(),
                    patient,
                    diagnosticDiseases);
            List<String> operationMessages = new ArrayList<>();
            String diagnosticMessage = diagnosticRepository.save(diagnostic);
            if (diagnosticMessage != null && !diagnosticMessage.isBlank()) {
                operationMessages.add(diagnosticMessage);
            }
            String highInfectivityMessage = highInfectivityPatientReportRepository.save(diagnostic);
            if (highInfectivityMessage != null && !highInfectivityMessage.isBlank()) {
                operationMessages.add(highInfectivityMessage);
            }
            String historyMessage = diagnosticHistoryRepository.save(diagnostic);
            if (historyMessage != null && !historyMessage.isBlank()) {
                operationMessages.add(historyMessage);
            }

            String response = SUCCESS_MESSAGE;
            if (!operationMessages.isEmpty()) {
                response = response + MESSAGE_SEPARATOR + String.join(MESSAGE_SEPARATOR, operationMessages);
            }
            System.out.println(response);
            return response;
        } catch (InvalidMessageFormatException e) {
            throw e;
        } catch (CorruptedDataException corruptedException) {
            System.out.println("[TCP][CorruptedData] " + corruptedException.getMessage());
            return "[TCP][Error] Error interno al procesar los datos del paciente";
        } catch (NotFoundException notFoundException) {
            String errorMessage = notFoundException.toTcpMessage();
            System.out.println(errorMessage);
            return errorMessage;
        } catch (DataValidationException exception) {
            String errorMessage = exception.toTcpMessage();
            System.out.println(errorMessage);
            return errorMessage;
        } catch (Exception e) {
            String errorMessage = "[TCP][Error] " + e.getMessage();
            System.out.println(errorMessage);
            return errorMessage;
        }
    }
}
