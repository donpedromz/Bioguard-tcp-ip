package com.donpedromz.data.patient;

/**
 * @author juanp
 * @version 1.0
 * Interfaz de configuración para el almacenamiento de pacientes en formato CSV.
 */
public interface ICsvStorageConfig {
    /**
     * Obtiene la ruta del archivo CSV donde se almacenan los datos de los pacientes.
     * @return la ruta del archivo CSV de pacientes configurada
     */
    String getPatientsCsvPath();
}