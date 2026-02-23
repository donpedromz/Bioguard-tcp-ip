package com.donpedromz.infrastructure.persistence.fasta;

/**
 * @version 1.0
 * @author juanp
 * Utilidad para manejar mensajes FASTA, proporcionando métodos para extraer secuencias genéticas y normalizar texto.
 */
public final class FastaUtils {
    /**
     * Extrae la secuencia genética (segunda línea) de un mensaje FASTA.
     * @param fastaMessage mensaje FASTA completo
     * @return secuencia genética en mayúsculas, o cadena vacía si el formato es inválido
     */
    public static String getSequenceFromFasta(String fastaMessage) {
        if (fastaMessage == null || fastaMessage.isBlank()) {
            return "";
        }
        String[] lines = fastaMessage.trim().split("\\R");
        if (lines.length < 2) {
            return "";
        }
        return lines[1].trim().toUpperCase();
    }

    /**
     * Retorna el texto recortado o cadena vacía si el valor es {@code null}.
     * @param value texto de entrada
     * @return texto normalizado sin espacios iniciales ni finales
     */
    public static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
