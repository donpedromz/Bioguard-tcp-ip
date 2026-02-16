package com.donpedromz.entities;

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
        if (value == null) {
            throw new IllegalArgumentException("Infectiousness no puede ser null");
        }
        return InfectiousnessLevel.valueOf(value.trim().toUpperCase());
    }
}