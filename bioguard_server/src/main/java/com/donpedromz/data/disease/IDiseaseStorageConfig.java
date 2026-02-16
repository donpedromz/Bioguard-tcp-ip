package com.donpedromz.data.disease;

/**
 * @author juanp
 * @version 1.0
 * Interfaz que representa la configuración necesaria para el almacenamiento de datos de enfermedades en formato FASTA.
 */
public interface IDiseaseStorageConfig {
    /**
     * Obtiene la ruta del directorio donde se almacenan los archivos FASTA
     * que contienen la información de las enfermedades.
     * @return La ruta del directorio de enfermedades como una cadena. No debe ser null ni vacío.
     */
    String getDiseasesDirectory();
}
