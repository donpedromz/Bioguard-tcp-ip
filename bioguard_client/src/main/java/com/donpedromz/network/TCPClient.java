package com.donpedromz.network;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define el contrato para un cliente TCP,
 * que es responsable de enviar mensajes a un servidor TCP y recibir respuestas.
 */
public interface TCPClient {
    /**
     * Envía un mensaje al servidor TCP y devuelve la respuesta recibida.
     * @param payload el mensaje que se desea enviar al servidor TCP, generalmente en formato de texto o binario,
     * @return un String que representa la respuesta recibida del servidor TCP, que puede contener información de
     * confirmación, resultados o cualquier otro tipo de datos que el servidor decida enviar como respuesta al
     * mensaje enviado por el cliente.
     */
    String send(String payload);
}
