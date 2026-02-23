package com.donpedromz.model;

import com.donpedromz.exceptions.ValidationException;

/**
 * @autor juanp
 * @version 1.0
 * Enumeración que representa el nivel de infecciosidad de un paciente. Los valores permitidos son:
 * - ALTA
 * - MEDIA
 * - BAJA
 */
public enum InfectiousnessLevel {
    ALTA,
    MEDIA,
    BAJA;

    /**
     * Convierte una cadena de texto en un valor de InfectiousnessLevel.
     * La cadena se normaliza eliminando espacios y convirtiendo a mayúsculas.
     * @param value La cadena de texto que representa el nivel de infecciosidad. No debe ser null ni vacío.
     * @return El valor de InfectiousnessLevel correspondiente a la cadena proporcionada.
     */
    public static InfectiousnessLevel from(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Infectiousness no puede ser vacío");
        }
        try {
            return InfectiousnessLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Nivel de infecciosidad inválido: '" + value + "'. Valores permitidos: ALTA, MEDIA, BAJA");
        }
    }
}