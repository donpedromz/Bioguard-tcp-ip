package com.donpedromz.network.config;

import com.donpedromz.common.IConfigReader;

/**
 * @version 1.0
 * @author juanp
 * Implementación de la configuración de red del cliente,
 * que adapta un lector de configuraciones genérico a la interfaz específica de SSL.
 */
public class SSLConfig implements ISSLConfig {
    /**
     * Clave de configuración para el host del servidor.
     */
    private static final String HOST_KEY = "server.host";
    /**
     * Clave de configuración para el puerto del servidor.
     */
    private static final String PORT_KEY = "server.port";

    /**
     * Clave de configuración para la ruta del trust-store SSL.
     */
    private static final String TRUSTSTORE_PATH_KEY = "ssl.truststore.path";

    /**
     * Clave de configuración para la contraseña del trust-store SSL.
     */
    private static final String TRUSTSTORE_PASSWORD_KEY = "ssl.truststore.password";

    /**
     * Lector de configuraciones genérico que proporciona acceso a los valores de configuración necesarios.
     */
    private final IConfigReader configReader;
    /**
     * Crea una nueva instancia de ClientNetworkConfig utilizando el lector de configuraciones proporcionado.
     * @param configReader reader that provides typed configuration values
     */
    public SSLConfig(IConfigReader configReader) {
        this.configReader = configReader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return configReader.getString(HOST_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return configReader.getInt(PORT_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTrustStorePath() {
        return configReader.getString(TRUSTSTORE_PATH_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTrustStorePassword() {
        return configReader.getString(TRUSTSTORE_PASSWORD_KEY);
    }
}
