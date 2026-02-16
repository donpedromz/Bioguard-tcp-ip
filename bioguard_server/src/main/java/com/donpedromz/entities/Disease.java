package com.donpedromz.entities;

import java.util.UUID;

/**
 * @author juanp
 * @version 1.0
 * Clase que representa la informacion de la entidad Disease
 */
public class Disease {
    private UUID uuid;
    private String diseaseName;
    private InfectiousnessLevel infectiousnessLevel;
    private String geneticSequence;

    /**
     * Constructor de la clase Disease.
     * @param diseaseName El nombre de la enfermedad. No debe ser null ni vacío.
     * @param infectiousnessLevel El nivel de infecciosidad de la enfermedad. No debe ser null.
     * @param geneticSequence La secuencia genética asociada a la enfermedad. No debe ser null ni vacío.
     */
    public Disease(String diseaseName, InfectiousnessLevel infectiousnessLevel, String geneticSequence) {
        this(null, diseaseName, infectiousnessLevel, geneticSequence);
    }

    /**
     * Constructor de la clase Disease con UUID.
     * @param uuid El UUID único de la enfermedad. No debe ser null.
     * @param diseaseName El nombre de la enfermedad. No debe ser null ni vacío.
     * @param infectiousnessLevel El nivel de infecciosidad de la enfermedad. No debe ser null.
     * @param geneticSequence La secuencia genética asociada a la enfermedad. No debe ser null ni vacío.
     */
    public Disease(UUID uuid, String diseaseName, InfectiousnessLevel infectiousnessLevel, String geneticSequence) {
        this.uuid = uuid;
        this.diseaseName = diseaseName;
        this.infectiousnessLevel = infectiousnessLevel;
        this.geneticSequence = geneticSequence;
    }
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public String getDiseaseName() {
        return diseaseName;
    }
    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }
    public InfectiousnessLevel getInfectiousnessLevel() {
        return infectiousnessLevel;
    }
    public void setInfectiousnessLevel(InfectiousnessLevel infectiousnessLevel) {
        this.infectiousnessLevel = infectiousnessLevel;
    }
    public String getGeneticSequence() {
        return geneticSequence;
    }
    public void setGeneticSequence(String geneticSequence) {
        this.geneticSequence = geneticSequence;
    }
    @Override
    public String toString() {
        return "Disease{" +
                "uuid=" + uuid +
                ", diseaseName='" + diseaseName + '\'' +
                ", infectiousnessLevel=" + infectiousnessLevel +
                ", geneticSequence='" + geneticSequence + '\'' +
                '}';
    }
}
