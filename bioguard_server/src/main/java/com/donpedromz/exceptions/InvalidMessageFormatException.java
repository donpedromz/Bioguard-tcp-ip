package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción personalizada para indicar que el formato del mensaje TCP recibido es inválido y no se puede procesar.
 */
public class InvalidMessageFormatException extends RuntimeException {
    private static final String TCP_PREFIX = "[TCP][400][InvalidFormat] ";

    public InvalidMessageFormatException(String message) {
        super(message);
    }
    public InvalidMessageFormatException(){
        super("Formato de mensaje invalido. No se puede procesar la solicitud.");
    }

    /**
     * Convierte el mensaje de error en formato TCP estandarizado.
     * @return mensaje con formato {@code [TCP][400][InvalidFormat] msg}
     */
    public String toTcpMessage() {
        return TCP_PREFIX + getMessage();
    }
}
