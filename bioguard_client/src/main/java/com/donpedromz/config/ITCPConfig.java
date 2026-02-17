package com.donpedromz.config;

/**
 * @version 1.0
 * @author juanp
 * Contrato que define los métodos necesarios para obtener la configuración de conexión TCP,
 * incluyendo el host y el puerto del servidor al que se desea conectar.
 */
public interface ITCPConfig {
    /**
     * Devuelve el host o dirección IP del servidor TCP al que se desea conectar.
     * @return host del servidor TCP
     */
    String getHost();

    /**
     * Devuelve el número de puerto del servidor TCP al que se desea conectar.
     * @return puerto del servidor TCP
     */
    int getPort();
}
