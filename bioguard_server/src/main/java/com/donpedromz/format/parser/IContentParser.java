package com.donpedromz.format.parser;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define el contrato para los parsers de contenido en el contexto de FASTA.
 * @param <T> El tipo de entidad que el parser debe producir a partir del contenido analizado.
 */
public interface IContentParser<T>{
    /**
     * Analiza el contenido de un mensaje y lo convierte en una entidad del tipo T.
     * @param body El contenido del mensaje que se desea analizar. No debe ser null.
     * @return Una instancia de la entidad del tipo T que representa el resultado del análisis del contenido.
     */
    T parse(String body);
}
