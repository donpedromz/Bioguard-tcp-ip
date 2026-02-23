package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando se detecta un error de validación de datos.
 * Esta excepción indica que los datos proporcionados no cumplen con las reglas de validación establecidas,
 * como por ejemplo, un diagnóstico que no tiene un identificador válido o que contiene información incompleta.
 */
public class ValidationException extends DataValidationException {
    public ValidationException(String message) {
        super(message);
    }
}
