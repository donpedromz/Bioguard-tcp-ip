package com.donpedromz.business;

import java.util.List;

/**
 * @version 1.0
 * @author juanp
 * Clase abstracta que define la estructura de una política de procesamiento para mensajes TCP.
 */
public abstract class ProcessingPolicy {
    /**
     * Lista de procesadores que se evaluarán en orden para procesar el mensaje recibido.
     */
    protected List<IMessageProcessor> processors;
    public ProcessingPolicy(List<IMessageProcessor> processors) {
        this.processors = processors;
    }

    /**
     * Aplica la cadena de procesadores al mensaje recibido. Cada procesador intentará procesar el mensaje y devolver una respuesta. Si un procesador no acepta el formato del mensaje, se lanzará una excepción que se capturará para intentar con el siguiente procesador. Si ningún procesador acepta el formato,
     * se devolverá un mensaje de error indicando que el formato es inválido.
     * @param message mensaje TCP recibido que se va a procesar.
     * @return Una cadena de texto con la respuesta del procesamiento o un mensaje de error si el formato es inválido.
     * @throws InvalidMessageFormatException cuando ningún procesador acepta el formato del mensaje recibido.
     */
    public abstract String apply(String message) throws InvalidMessageFormatException;
}
