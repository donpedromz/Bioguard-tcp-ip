package com.donpedromz.infrastructure.network;

/**
 * @author juanp
 * @version 1.0
 * Interfaz que representa la configuración necesaria para establecer una conexión SSL. Extiende la interfaz ITCPConfig.
 */
public interface ISSLConfig extends ITCPConfig{
    /**
     * Obtiene la ruta del archivo de almacén de claves (keystore)
     * que contiene los certificados necesarios para la conexión SSL.
     * @return La ruta del archivo de almacén de claves (keystore) como una cadena. No debe ser null ni vacío.
     */
    String getKeyStorePath();

    /**
     * Obtiene la contraseña del almacén de claves (keystore)
     * que se utiliza para acceder a los certificados necesarios para la conexión SSL.
     * @return La contraseña del almacén de claves (keystore) como una cadena. No debe ser null ni vacío.
     */
    String getKeyStorePassword();
}
