package com.donpedromz.network.config;

/**
 * @version 1.0
 * @author juanp
 * Contrato que extiende ITCPConfig para incluir configuraciones específicas de SSL,
 * como la ruta y contraseña del trust-store.
 */
public interface ISSLConfig extends ITCPConfig {
    /**
     * Devuelve la ruta al archivo del trust-store SSL,
     * que contiene los certificados de confianza necesarios para establecer conexiones seguras.
     * @return ruta al trust-store SSL
     */
    String getTrustStorePath();

    /**
     * Devuelve la contraseña para acceder al trust-store SSL,
     * @return contraseña del trust-store SSL
     */
    String getTrustStorePassword();
}
