package com.donpedromz.fasta.file;

/**
 * @version 1.0
 * @author juanp
 * Excepción personalizada que se lanza cuando ocurre un error durante el escaneo o procesamiento de archivos FASTA,
 * como problemas de formato, lectura o validación de secuencias genéticas.
 */
public class FastaScanException extends RuntimeException {
    public FastaScanException(String message) {
        super(message);
    }

    public FastaScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
