package com.donpedromz.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @version 1.0
 * @author juanp
 * Clase que implementa la interfaz IConfigReader para leer configuraciones desde un archivo de propiedades.
 */
public class PropertiesManager implements IConfigReader {
    /**
     * Objeto Properties que almacena las configuraciones cargadas desde el archivo.
     */
    private final Properties properties = new Properties();

    /**
     * Constructor que carga las configuraciones desde un archivo de propiedades ubicado en el classpath.
     * El archivo debe estar presente y ser accesible, de lo contrario se lanzará una excepción.
     * @param fileName nombre del archivo de propiedades a cargar (e.g. "config.properties")
     */
    public PropertiesManager(String fileName) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Configuration file not found: " + fileName);
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load configuration: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration key: " + key);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }
}
