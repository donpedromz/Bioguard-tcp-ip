package com.donpedromz.common;

/**
 * @version 1.0
 * @author juanp
 * Utilidad para operaciones comunes con texto, como sanitización y validación.
 */
public final class TextUtils {
    /**
     * Sanitiza el texto eliminando espacios en blanco al inicio y al final, y reemplazando null por una cadena vacía.
     * @param value texto a sanitizar
     * @return texto sanitizado, o una cadena vacía si el valor original era null
     */
    public static String sanitizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
