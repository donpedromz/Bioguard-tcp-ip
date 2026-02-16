package com.donpedromz.business;

/**
 * @version 1.0
 * @author juanp
 * Excepción personalizada para errores de validación de datos en el procesamiento de mensajes TCP.
 */
public class DataValidationException extends RuntimeException {
    /**
     * Prefijo utilizado en los mensajes de error TCP para indicar que se trata de un error de validación de datos.
     */
    private static final String TCP_PREFIX = "[TCP][ValidationError] ";
    public DataValidationException(String message) {
        super(message);
    }
    public String toTcpMessage() {
        return TCP_PREFIX + getMessage();
    }
}