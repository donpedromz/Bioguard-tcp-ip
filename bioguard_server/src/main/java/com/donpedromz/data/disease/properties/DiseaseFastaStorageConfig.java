package com.donpedromz.data.disease.properties;

import com.donpedromz.common.IConfigReader;
import com.donpedromz.data.disease.IDiseaseFastaStorageConfig;

/**
 * @version 1.0
 * @author juanp
 * Implementación de la interfaz IDiseaseFastaStorageConfig que obtiene la
 * configuración de almacenamiento de enfermedades.
 */
public class DiseaseFastaStorageConfig implements IDiseaseFastaStorageConfig {
    /**
     * Clave de configuración para la ruta del directorio de enfermedades dentro del application.properties.
     */
    private static final String DISEASES_DIRECTORY_KEY = "storage.diseases.directory";
    /**
     * Fuente de configuración utilizada para leer los valores necesarios
     * para configurar el almacenamiento de enfermedades.
     */
    private final IConfigReader configReader;
    /**
     * Crea una nueva instancia de DiseaseFastaStorageConfig utilizando un lector de configuración.
     * @param configReader Lector de configuración que proporciona acceso a las propiedades necesarias para
     *                     configurar el almacenamiento de enfermedades.
     */
    public DiseaseFastaStorageConfig(IConfigReader configReader) {
        if (configReader == null) {
            throw new IllegalArgumentException("configReader no puede ser null");
        }
        this.configReader = configReader;
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public String getDiseasesDirectory() {
        String value = configReader.getString(DISEASES_DIRECTORY_KEY);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad de configuración: " + DISEASES_DIRECTORY_KEY);
        }
        return value.trim();
    }
}
