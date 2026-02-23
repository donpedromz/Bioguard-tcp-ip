package com.donpedromz.infrastructure.integrity;

import com.donpedromz.exceptions.CorruptedDataException;

import java.nio.file.Path;

/**
 * @version 1.0
 * @author juanp
 * Interfaz para verificar la integridad de datos mediante el cálculo de hashes y la validación de archivos.
 */
public interface IntegrityVerifier {
    /**
     * Calcula el hash de un texto.
     * @param value texto de entrada
     * @return hash en representación hexadecimal en minúsculas
     */
    String computeHash(String value);
    /**
     * Verifica la integridad de un archivo cuyo nombre (sin extensión) debe coincidir
     * con el hash de su contenido.
     * @param filePath  ruta del archivo a verificar
     * @param extension extensión del archivo (e.g. {@code ".fasta"})
     * @throws CorruptedDataException si el archivo está vacío,
     *         no se puede leer, o el hash del contenido no coincide con el nombre
     */
    void verifyFileIntegrity(Path filePath, String extension);
}
