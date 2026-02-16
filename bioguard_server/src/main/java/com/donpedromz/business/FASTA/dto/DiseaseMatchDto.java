package com.donpedromz.business.FASTA.dto;

import com.donpedromz.entities.Disease;

/**
 * @version 1.0
 * @author juanp
 * DTO Que reperesenta un resultado de coicnidencia dentro de un diagnostico.
 */
public class DiseaseMatchDto {
    private final Disease disease;
    private final int startPosition;
    private final int endPosition;
    public DiseaseMatchDto(Disease disease, int startPosition, int endPosition) {
        this.disease = disease;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    public Disease disease() {
        return disease;
    }
    public int startPosition() {
        return startPosition;
    }
    public int endPosition() {
        return endPosition;
    }
}
