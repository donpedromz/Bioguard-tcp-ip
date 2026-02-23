package com.donpedromz.infrastructure.network;

import com.donpedromz.common.IConfigReader;

/**
 * @version 1.0
 * @author juanp
 * Clase que implementa la interfaz ISSLConfig para proporcionar la configuración
 * necesaria para establecer una conexión TCP segura utilizando SSL.
 */
public class TCPConfig implements ISSLConfig{
    /**
     * El objeto IConfigReader se utiliza para leer la configuración desde una fuente externa,
     * como un archivo de propiedades o una base de datos.
     */
    private final IConfigReader configReader;
    public TCPConfig(IConfigReader configReader){
        this.configReader = configReader;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public int getPort() {
        return configReader.getInt("server.port");
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public String getKeyStorePath() {
        return configReader.getString("ssl.keystore.path");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKeyStorePassword() {
        return configReader.getString("ssl.keystore.password");
    }
}
