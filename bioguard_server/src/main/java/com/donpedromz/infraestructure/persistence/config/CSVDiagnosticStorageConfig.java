package com.donpedromz.infraestructure.persistence.config;

import com.donpedromz.common.IConfigReader;
import com.donpedromz.repositories.diagnostic.properties.IDiagnosticStorageConfig;

/**
 * @author juanp
 * @version 1.0
 * Implementación de la configuración para el almacenamiento de diagnósticos.
 * Esta clase se encarga de leer las propiedades necesarias para configurar el almacenamiento de diagnósticos
 * desde una fuente de configuración proporcionada por un IConfigReader.
 */
public class CSVDiagnosticStorageConfig implements IDiagnosticStorageConfig {
    /**
     * Clave de configuración para la ruta del directorio de diagnósticos dentro del application.properties
     */
    private static final String DIAGNOSTICS_DIRECTORY_KEY = "storage.diagnostics.directory";
    /**
     * Clave de configuración para la ruta del directorio
     * de informes de alta infecciosidad dentro del application.properties
     */
    private static final String HIGH_INFECTIOUSNESS_REPORTS_DIRECTORY_KEY
            = "storage.reports.high_infectiousness.directory";
    /**
     * Fuente de configuración utilizada para leer los valores
     * necesarios para configurar el almacenamiento de diagnósticos.
     */
    private final IConfigReader configReader;

    /**
     * Crea una nueva instancia de DiagnosticStorageConfig utilizando un lector de configuración.
     * @param configReader Lector de configuración que proporciona acceso a las propiedades
     *                     necesarias para configurar el almacenamiento de diagnósticos.
     */
    public CSVDiagnosticStorageConfig(IConfigReader configReader) {
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
    public String getDiagnosticsPath() {
        return getRequiredPath(DIAGNOSTICS_DIRECTORY_KEY);
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public String getHighInfectiousnessReportsPath() {
        return getRequiredPath(HIGH_INFECTIOUSNESS_REPORTS_DIRECTORY_KEY);
    }

    /**
     * Obtiene una ruta de directorio requerida a partir de la configuración utilizando la clave proporcionada.
     * @param key La clave de configuración para la ruta del directorio que se desea obtener.
     * @return La ruta del directorio configurada para la clave proporcionada. No debe ser null ni vacío.
     */
    private String getRequiredPath(String key) {
        String value = configReader.getString(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la propiedad de configuración: " + key);
        }
        return value.trim();
    }
}
