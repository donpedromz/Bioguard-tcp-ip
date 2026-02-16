package com.donpedromz.common;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define el contrato para la lectura de configuraciones.
 */
public interface IConfigReader {
    /**
     * Lee el valor de configuración asociado a la clave proporcionada.
     * @param key clave de configuración para la cual se desea obtener el valor.
     * @return El valor de configuración asociado a la clave proporcionada.
     */
    String getString(String key);

    /**
     * Lee el valor de configuración asociado a la clave proporcionada y lo convierte a un entero.
     * @param key clave de configuración para la cual se desea obtener el valor entero.
     * @return El valor de configuración asociado a la clave proporcionada convertido a un entero.
     */
    int getInt(String key);

    /**
     * Lee el valor de configuración asociado a la clave proporcionada y lo convierte a un booleano.
     * @param key clave de configuración para la cual se desea obtener el valor booleano.
     * @return El valor de configuración asociado a la clave proporcionada convertido a un booleano.
     */
    boolean getBoolean(String key);
}
