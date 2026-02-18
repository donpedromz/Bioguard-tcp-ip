package com.donpedromz.domain.diagnostic;

import java.util.Locale;

import static com.donpedromz.common.TextUtils.sanitizeText;

/**
 * @version 1.0
 * @author juanp
 * Clase que representa el registro de un diagnóstico,
 * incluyendo el documento del paciente, la fecha de la muestra y la secuencia genética.
 */
public class DiagnosticRegistration {
    /**
     * Documento de identidad del paciente asociado al diagnóstico.
     */
    private final String patientDocument;
    /**
     * Fecha de la muestra en formato YYYY-MM-DD.
     */
    private final String sampleDate;
    /**
     * Secuencia genética de la muestra, compuesta por caracteres ACGT.
     */
    private final String geneticSequence;

    public DiagnosticRegistration(String patientDocument, String sampleDate, String geneticSequence) {
        this.patientDocument = sanitizeText(patientDocument);
        this.sampleDate = sanitizeText(sampleDate);
        this.geneticSequence = sanitizeSequence(geneticSequence);
    }

    private static String sanitizeSequence(String sequence) {
        return sanitizeText(sequence).toUpperCase(Locale.ROOT);
    }

    public String getPatientDocument() {
        return patientDocument;
    }

    public String getSampleDate() {
        return sampleDate;
    }

    public String getGeneticSequence() {
        return geneticSequence;
    }
}
