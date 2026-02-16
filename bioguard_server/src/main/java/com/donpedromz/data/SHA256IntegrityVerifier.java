package com.donpedromz.data;

import com.donpedromz.data.exceptions.CorruptedDataException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @version 1.0
 * @author juanp
 * Implementación de {@link IntegrityVerifier} que utiliza el algoritmo
 * SHA-256 para calcular hashes y verificar la integridad de archivos.
 */
public class SHA256IntegrityVerifier implements IntegrityVerifier {
    /**
     * Calcula el hash SHA-256 de un texto.
     * @param value texto de entrada
     * @return hash SHA-256 en representación hexadecimal en minúsculas
     * @throws IllegalStateException si el algoritmo SHA-256 no está disponible en la JVM
     */
    @Override
    public String computeHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", exception);
        }
    }
    /**
     * Verifica la integridad de un archivo cuyo nombre (sin extensión) debe coincidir
     * con el hash SHA-256 de su contenido.
     * @param filePath  ruta del archivo a verificar
     * @param extension extensión del archivo (e.g. {@code ".fasta"})
     * @throws CorruptedDataException si el archivo está vacío, no se puede leer,
     *                                o el hash del contenido no coincide con el nombre
     */
    @Override
    public void verifyFileIntegrity(Path filePath, String extension) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content == null || content.isBlank()) {
                throw new CorruptedDataException(
                        "Archivo vacío o ilegible: " + filePath.getFileName() + " en " + filePath.toAbsolutePath()
                );
            }
            String fileName = filePath.getFileName().toString();
            String expectedHash = fileName.endsWith(extension)
                    ? fileName.substring(0, fileName.length() - extension.length())
                    : fileName;
            String actualHash = computeHash(content);
            if (!expectedHash.equals(actualHash)) {
                throw new CorruptedDataException(
                        "Archivo corrupto o modificado: " + fileName + " en " + filePath.toAbsolutePath()
                );
            }
        } catch (IOException exception) {
            throw new CorruptedDataException(
                    "Error al leer archivo: " + filePath.getFileName() + " en " + filePath.toAbsolutePath(),
                    exception
            );
        }
    }
}
