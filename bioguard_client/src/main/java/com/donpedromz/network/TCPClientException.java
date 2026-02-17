package com.donpedromz.network;

/**
 * @version 1.0
 * @author juanp
 * Excepción personalizada que se lanza cuando ocurre un error durante la comunicación con el servidor TCP,
 * como problemas de conexión, envío o recepción de mensajes,
 * o cualquier otro tipo de error relacionado con el cliente TCP.
 */
public class TCPClientException extends RuntimeException {
    /**
     * Constructor que acepta un mensaje de error como argumento,
     * permitiendo proporcionar información detallada sobre la naturaleza del
     * error que ocurrió durante la comunicación con el servidor TCP.
     * @param message el mensaje de error que describe la causa del problema,
     * @param cause la causa raíz del error, que puede ser otra excepción
     *              que se lanzó durante el proceso de comunicación con el servidor TCP,
     */
    public TCPClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
