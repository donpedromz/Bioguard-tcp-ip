package com.donpedromz.network;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que representa la configuración necesaria para establecer una conexión TCP.
 * Define el método getPort() para obtener el número de puerto utilizado en la conexión TCP.
 */
public interface ITCPConfig {
    /**
     * Obtiene el número de puerto utilizado para la conexión TCP.
     * @return El número de puerto como un entero. Debe ser un valor válido entre 1 y 65535. No debe ser null ni vacío.
     */
    int getPort();
}
