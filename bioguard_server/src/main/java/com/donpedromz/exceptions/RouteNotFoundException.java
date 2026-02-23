package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando no se encuentra un procesador para la ruta solicitada
 * en la tabla de enrutamiento del servidor.
 */
public class RouteNotFoundException extends RuntimeException {
    private static final String TCP_PREFIX = "[TCP][404][RouteNotFound] ";

    /**
     * Crea una nueva instancia con un mensaje describiendo la ruta no encontrada.
     * @param message descripción de la ruta que no fue encontrada.
     */
    public RouteNotFoundException(String message) {
        super(message);
    }

    /**
     * Devuelve el mensaje formateado para respuesta TCP.
     * @return mensaje con formato {@code [TCP][404][RouteNotFound] msg}
     */
    public String toTcpMessage() {
        return TCP_PREFIX + getMessage();
    }
}
