package com.donpedromz.network;

/**
 * @author juanp
 * @version 1.0
 * Interfaz que representa el servicio de red para la aplicación BioGuard. Define las operaciones relacionadas
 * con la comunicación en red, como el envío de datos a un servidor TCP.
 * Esta interfaz puede ser implementada por diferentes clases para proporcionar diversas formas de comunicación en red,
 * como TCP, HTTP, etc.
 */
public interface INetworkService {
    void start();
}
