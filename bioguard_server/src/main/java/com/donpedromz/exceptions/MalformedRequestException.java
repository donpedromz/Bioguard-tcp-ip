package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando el mensaje TCP recibido no tiene la estructura esperada
 * para ser enrutado (no se pueden extraer método, content-type y cuerpo).
 */
public class MalformedRequestException extends RuntimeException {
    private static final String TCP_PREFIX = "[TCP][400][MalformedRequest] ";

    /**
     * Crea una nueva instancia con un mensaje describiendo el problema de formato.
     * @param message descripción del error de formato del mensaje.
     */
    public MalformedRequestException(String message) {
        super(message);
    }

    /**
     * Devuelve el mensaje formateado para respuesta TCP.
     * @return mensaje con formato {@code [TCP][400][MalformedRequest] msg}
     */
    public String toTcpMessage() {
        return TCP_PREFIX + getMessage();
    }
}
