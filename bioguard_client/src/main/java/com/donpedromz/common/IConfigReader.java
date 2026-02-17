package com.donpedromz.common;

/**
 * @version 1.0
 * @author juanp
 * Contrato que define métodos para leer configuraciones de diferentes fuentes (archivos, variables de entorno, etc.).
 */
public interface IConfigReader {

    /**
     * Recupera un valor de configuración como una cadena de texto.
     * @param key clave de configuración
     * @return valor de configuración asociado a la clave, o null si no se encuentra
     */
    String getString(String key);

    /**
     * Recupera un valor de configuración como un entero.
     * @param key clave de configuración
     * @return valor de configuración asociado a la clave, convertido a entero
     */
    int getInt(String key);

    /**
     * Recupera un valor de configuración como un booleano.
     * @param key clave de configuración
     * @return valor de configuración asociado a la clave, convertido a booleano
     */
    boolean getBoolean(String key);
}
