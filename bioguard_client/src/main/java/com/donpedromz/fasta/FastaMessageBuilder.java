package com.donpedromz.fasta;

/**
 * @version 1.0
 * @author juanp
 * Interfaz funcional que define un contrato para construir mensajes en formato FASTA a partir de un payload genérico.
 * @param <T> el tipo de payload
 *           que se desea convertir en un mensaje FASTA, puede ser cualquier clase o estructura de datos
 */
public interface FastaMessageBuilder<T> {
    /**
     * Construye un mensaje en formato FASTA a partir del payload proporcionado.
     * @param payload el objeto de tipo T que contiene la información necesaria para construir el mensaje FASTA,
     * @return un String que representa el mensaje en formato FASTA,
     * que generalmente incluye un encabezado seguido de una secuencia genética,
     */
    String build(T payload);
}
