package com.donpedromz.data.diagnostic;

/**
 * @version 1.0
 * @author juanp
 * Interfaz de configuración para el almacenamiento de diagnósticos. Esta interfaz define los métodos necesarios
 * para obtener las rutas de los directorios donde se almacenan los diagnósticos y los informes de alta infecciosidad.
 */
public interface IDiagnosticStorageConfig {
    /**
     * Obtiene la ruta del directorio donde se almacenan los diagnósticos.
     * @return la ruta del directorio de diagnósticos configurada
     */
    String getDiagnosticsPath();

    /**
     * Obtiene la ruta del directorio donde se almacenan los informes de alta infecciosidad.
     * @return la ruta del directorio de informes de alta infecciosidad configurada
     */
    String getHighInfectiousnessReportsPath();
}
