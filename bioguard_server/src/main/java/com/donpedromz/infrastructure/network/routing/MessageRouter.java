package com.donpedromz.infrastructure.network.routing;

import java.util.Map;

import com.donpedromz.controllers.IMessageProcessor;
import com.donpedromz.infrastructure.network.data.Response;
/**
 * @version 2.0
 * @author juanp
 * Clase abstracta que define la estructura de un router de mensajes TCP.
 * Contiene una tabla de enrutamiento que mapea rutas a procesadores de mensajes.
 */
public abstract class MessageRouter {
    /**
     * Tabla de enrutamiento que mapea rutas (strings) a procesadores de mensajes (IMessageProcessor).
     * Se utiliza para despachar mensajes recibidos a los controladores correspondientes.
     */
    protected Map<String, IMessageProcessor> routingTable;
    /**
     * Constructor que inicializa la tabla de enrutamiento.
     * @param routingTable Tabla de enrutamiento que mapea rutas a procesadores. No debe ser null.
     */
    public MessageRouter(Map<String, IMessageProcessor> routingTable) {
        this.routingTable = routingTable;
    }
    /**
     * Método abstracto que debe ser implementado por las subclases para despachar un mensaje TCP
     * a un procesador específico según la lógica de enrutamiento definida.
     * @param message mensaje TCP completo recibido del cliente.
     * @return respuesta TCP generada por el controlador o una respuesta de error.
     */
    public abstract Response dispatchMessage(String message);
}
