package com.donpedromz.service;

import com.donpedromz.dtos.DiagnoseMessageDto;
import com.donpedromz.exceptions.InvalidFastaFormatException;
import com.donpedromz.dtos.DiagnoseResult;
import com.donpedromz.repositories.diagnostic.IDiagnosticHistoryRepository;
import com.donpedromz.repositories.diagnostic.IHighInfectivityPatientReportRepository;
import com.donpedromz.repositories.diagnostic.IDiagnosticRepository;
import com.donpedromz.repositories.disease.IDiseaseRepository;
import com.donpedromz.exceptions.ConflictException;
import com.donpedromz.exceptions.NotFoundException;
import com.donpedromz.repositories.patient.IPatientRepository;
import com.donpedromz.model.Diagnostic;
import com.donpedromz.model.Disease;
import com.donpedromz.model.Patient;

import com.donpedromz.exceptions.ValidationException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @version 1.0
 * @author juanp
 * Servicio de negocio que implementa la lógica de diagnóstico a partir de muestras genéticas.
 * Busca coincidencias entre la secuencia genética de la muestra del paciente y las enfermedades registradas,
 * persiste el diagnóstico y genera reportes adicionales cuando corresponde.
 */
public class DiagnoseService implements IDiagnoseService {
    private static final String DOCUMENT_REGEX = "^\\d+$";
    private static final String SEQUENCE_REGEX = "^[ACGT]+$";
    private static final int MIN_DIAGNOSE_SEQUENCE_LENGTH = 7;
    private static final int MAX_DOCUMENT_LENGTH = 20;
    private static final int MAX_DIAGNOSE_SEQUENCE_LENGTH = 5000;
    private final IPatientRepository patientRepository;
    private final IDiseaseRepository diseaseRepository;
    private final IDiagnosticRepository diagnosticRepository;
    private final IHighInfectivityPatientReportRepository highInfectivityPatientReportRepository;
    private final IDiagnosticHistoryRepository diagnosticHistoryRepository;

    /**
     * Construye el servicio de diagnóstico con los repositorios necesarios.
     * @param patientRepository repositorio de pacientes
     * @param diseaseRepository repositorio de enfermedades
     * @param diagnosticRepository repositorio de diagnósticos
     * @param highInfectivityPatientReportRepository repositorio de reportes de alta infectividad
     * @param diagnosticHistoryRepository repositorio de historial de diagnósticos
     */
    public DiagnoseService(
            IPatientRepository patientRepository,
            IDiseaseRepository diseaseRepository,
            IDiagnosticRepository diagnosticRepository,
            IHighInfectivityPatientReportRepository highInfectivityPatientReportRepository,
            IDiagnosticHistoryRepository diagnosticHistoryRepository
    ) {
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
     * {@inheritDoc}
     */
    @Override
    public DiagnoseResult diagnose(DiagnoseMessageDto diagnoseMessage) {
        if (diagnoseMessage.patientDocument() == null || diagnoseMessage.sampleDate() == null
                || diagnoseMessage.geneticSequence() == null) {
            throw new InvalidFastaFormatException("Diagnose FASTA contains null values");
        }

        List<String> invalidFields = new ArrayList<>();
        if (!diagnoseMessage.patientDocument().matches(DOCUMENT_REGEX)) {
            invalidFields.add("patientDocument");
        } else if (diagnoseMessage.patientDocument().length() > MAX_DOCUMENT_LENGTH) {
            invalidFields.add("patientDocument (máximo " + MAX_DOCUMENT_LENGTH + " caracteres)");
        }
        try {
            LocalDate.parse(diagnoseMessage.sampleDate());
        } catch (DateTimeParseException e) {
            invalidFields.add("sampleDate");
        }
        String sequence = diagnoseMessage.geneticSequence();
        if (!sequence.matches(SEQUENCE_REGEX)) {
            invalidFields.add("sampleSequence");
        } else if (sequence.length() < MIN_DIAGNOSE_SEQUENCE_LENGTH) {
            invalidFields.add("sampleSequence (mínimo " + MIN_DIAGNOSE_SEQUENCE_LENGTH + " nucleótidos)");
        } else if (sequence.length() > MAX_DIAGNOSE_SEQUENCE_LENGTH) {
            invalidFields.add("sampleSequence (máximo " + MAX_DIAGNOSE_SEQUENCE_LENGTH + " nucleótidos)");
        }
        if (!invalidFields.isEmpty()) {
            throw new ValidationException("Campos inválidos: " + String.join(", ", invalidFields));
        }

        Patient patient = patientRepository.getByDocument(diagnoseMessage.patientDocument());
        if (patient == null) {
            throw new NotFoundException("no se encontró ningún paciente con dicho documento");
        }
        if (patient.getUuid() == null) {
            throw new NotFoundException("no se encontró UUID para el paciente del documento enviado");
        }

        if (diagnosticRepository.existsByPatientAndSample(
                patient.getUuid(),
                diagnoseMessage.geneticSequence(),
                diagnoseMessage.sampleDate(),
                diagnoseMessage.patientDocument())) {
            throw new ConflictException("ya existe un diagnóstico registrado para este paciente con la misma muestra y fecha");
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

        return new DiagnoseResult(diagnostic, operationMessages);
    }

    /**
     * Busca coincidencias exactas de la secuencia del paciente en todas las enfermedades.
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
}
