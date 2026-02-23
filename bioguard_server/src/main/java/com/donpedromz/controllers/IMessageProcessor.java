package com.donpedromz.controllers;

import com.donpedromz.infraestructure.network.data.Request;
import com.donpedromz.infraestructure.network.data.Response;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define el contrato para los procesadores de mensajes TCP.
 */
public interface IMessageProcessor{
    /**
     * Procesa el mensaje recibido y devuelve una respuesta si el formato es válido, o lanza una excepción si no lo es.
     * @param request mensaje TCP recibido que se va a procesar.
     * @return Una respuesta del tipo Response con el resultado del procesamiento.
     */
    Response process(Request request);
}
