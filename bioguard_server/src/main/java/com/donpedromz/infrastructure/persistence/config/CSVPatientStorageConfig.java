package com.donpedromz.infrastructure.persistence.config;

import com.donpedromz.common.IConfigReader;
import com.donpedromz.repositories.patient.properties.IPatientStorageConfig;

/**
 * @author juanp
 * @version 1.0
 * Implementación de la configuración para el almacenamiento de pacientes en formato CSV.
 */
public class CSVPatientStorageConfig implements IPatientStorageConfig {
    /**
     * Clave de configuración para la ruta del archivo CSV de pacientes dentro del application.properties
     */
    private static final String PATIENTS_CSV_KEY = "storage.csv.patients.path";
    /**
     * Fuente de configuración utilizada para leer los valores necesarios para configurar el almacenamiento CSV.
     */
    private final IConfigReader configReader;

    /**
     * Crea una nueva instancia de CsvStorageConfig utilizando un lector de configuración.
     * @param configReader Lector de configuración que proporciona acceso a
     *                     las propiedades necesarias para configurar el almacenamiento CSV.
     */
    public CSVPatientStorageConfig(IConfigReader configReader) {
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
    public String getPatientStoragePath() {
        return getRequiredPath();
    }

    /**
     * Obtiene una ruta de archivo requerida a partir de la configuración.
     * Si la propiedad no está presente o es inválida, se lanza una excepción.
     * @return la ruta del archivo CSV de pacientes configurada
     */
    private String getRequiredPath() {
        String value = configReader.getString(CSVPatientStorageConfig.PATIENTS_CSV_KEY);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad de configuración: " + CSVPatientStorageConfig.PATIENTS_CSV_KEY);
        }
        return value.trim();
    }
}