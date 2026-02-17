package com.donpedromz.domain.disease;

import java.util.Locale;

import static com.donpedromz.common.TextUtils.sanitizeText;

/**
 * @version 1.0
 * @author juanp
 * Clase que representa el registro de una enfermedad,
 * incluyendo su nombre, nivel de contagiosidad y secuencia genética.
 */
public class DiseaseRegistration {
    private final String name;
    private final String infectiousnessLevel;
    private final String sequence;

    public DiseaseRegistration(String name, String infectiousnessLevel, String sequence) {
        this.name = sanitizeText(name);
        this.infectiousnessLevel = sanitizeText(infectiousnessLevel);
        this.sequence = sanitizeSequence(sequence);
    }

    private static String sanitizeSequence(String sequence) {
        return sanitizeText(sequence).toUpperCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    public String getInfectiousnessLevel() {
        return infectiousnessLevel;
    }

    public String getSequence() {
        return sequence;
    }
}
