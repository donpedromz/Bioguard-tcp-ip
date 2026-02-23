package com.donpedromz.repositories.patient.properties;

/**
 * @author juanp
 * @version 1.0
 * Interfaz de configuración para el almacenamiento de pacientes en formato CSV.
 */
public interface IPatientStorageConfig {
    /**
     * Obtiene la ruta del archivo CSV donde se almacenan los datos de los pacientes.
     * @return la ruta del archivo CSV de pacientes configurada
     */
    String getPatientStoragePath();
}