package com.donpedromz.dtos;

/**
 * @version 1.0
 * @author juanp
 * DTO que encapsula la informacion recibida en el mensaje TCP para un diagnostico.
 */
public class DiagnoseMessageDto {
    private final String patientDocument;
    private final String sampleDate;
    private final String geneticSequence;
    public DiagnoseMessageDto(
            String patientDocument,
            String sampleDate,
            String geneticSequence
    ) {
        this.patientDocument = patientDocument;
        this.sampleDate = sampleDate;
        this.geneticSequence = geneticSequence;
    }
    public String patientDocument() {
        return patientDocument;
    }
    public String sampleDate() {
        return sampleDate;
    }
    public String geneticSequence() {
        return geneticSequence;
    }
}
