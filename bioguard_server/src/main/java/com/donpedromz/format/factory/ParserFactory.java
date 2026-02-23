package com.donpedromz.format.factory;
/**
 * @version 1.0
 * @author juanp
 * Interfaz que define el contrato para las fábricas de parsers en el contexto de procesamiento de mensajes TCP con diferentes tipos de contenido.
 */
public interface ParserFactory<T>{
    /**
     * Registra un parser específico para un tipo de contenido dado.
     * @param contentype El tipo de contenido (por ejemplo, "application/fasta") para el cual se desea registrar el parser. No debe ser null ni vacío.
     * @param body El cuerpo del mensaje que se desea analizar. No debe ser null.
     * @return Una instancia de la entidad del tipo T que representa el resultado del análisis del contenido. El tipo de entidad dependerá del parser registrado para el content-type especificado.
     */
    T parse(String contentype, String body);
}
