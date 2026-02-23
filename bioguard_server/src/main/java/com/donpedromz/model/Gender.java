package com.donpedromz.model;

import com.donpedromz.exceptions.ValidationException;

import java.util.Locale;

/**
 * @autor juanp
 * @version 1.0
 * Enumeración que representa el género de un paciente. Los valores permitidos son:
 * - MASCULINO
 * - FEMENINO
 * - OTRO
 * - NO_ESPECIFICADO
 */
public enum Gender {
    MASCULINO("MASCULINO"),
    FEMENINO("FEMENINO"),
    OTRO("OTRO"),
    NO_ESPECIFICADO("NO ESPECIFICADO");
    private final String value;
    Gender(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

    /**
     * Convierte una cadena de texto en un genero.
     * La cadena se normaliza eliminando espacios y convirtiendo a mayúsculas.
     * @param rawValue La cadena de texto que representa el género.
     *                 No debe ser null ni vacío. Se normaliza eliminando espacios y convirtiendo a mayúsculas.
     * @return genero correspondiente a la cadena proporcionada.
     */
    public static Gender fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new ValidationException("Gender no puede ser vacío");
        }
        String normalized = rawValue.trim()
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        try {
            return Gender.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Gender inválido. Valores permitidos: MASCULINO, FEMENINO, OTRO, NO ESPECIFICADO");
        }
    }
}