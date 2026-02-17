package com.donpedromz.fasta.file;

/**
 * @version 1.0
 * @author juanp
 * Clase genérica que representa un ítem de escaneo de archivos FASTA,
 * incluyendo el nombre del archivo y un payload de tipo genérico T que puede contener
 * cualquier información adicional relacionada con el escaneo, como resultados, metadatos o información de contexto.
 * @param <T> el tipo de payload que se desea asociar con el ítem de escaneo, puede ser cualquier clase o estructura de
 *           datos que se necesite para almacenar información adicional sobre el proceso de escaneo de archivos FASTA.
 */
public class FastaScanItem<T> {
    private final String fileName;
    private final T payload;
    public FastaScanItem(String fileName, T payload) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName is required");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
        this.fileName = fileName;
        this.payload = payload;
    }
    public String getFileName() {
        return fileName;
    }
    public T getPayload() {
        return payload;
    }
}
