package com.donpedromz.business.FASTA;

import com.donpedromz.business.IMessageProcessor;
import com.donpedromz.business.InvalidMessageFormatException;
import com.donpedromz.business.ProcessingPolicy;
import com.donpedromz.business.FASTA.exceptions.InvalidFastaFormatException;

import java.util.List;

/**
 * @version 1.0
 * @author juanp
 * Clase que implementa la política de procesamiento para mensajes en formato FASTA.
 */
public class FASTAProcessor extends ProcessingPolicy {
    /**
     * Crea una política FASTA con la lista de procesadores disponibles
     * @param processors procesadores que se evaluarán en orden
     */
    public FASTAProcessor(List<IMessageProcessor> processors) {
        super(processors);   
    }
    /**
     * Aplica la cadena de procesadores al mensaje recibido.
     * @param message mensaje FASTA recibido por TCP
     * @return respuesta del primer procesador que acepte el mensaje
     * @throws InvalidFastaFormatException cuando ningún procesador acepta el formato
     */
    @Override
    public String apply(String message){
        InvalidMessageFormatException lastFormatException = null;
        for(IMessageProcessor processor : processors){
            try {
                String response = processor.process(message);
                if (response != null) {
                    return response;
                }
            } catch (InvalidMessageFormatException exception) {
                lastFormatException = exception;
            }
        }
        String errorMessage = "[TCP][FormatError] formato fasta invalido";
        if (lastFormatException != null && lastFormatException.getMessage() != null
                && !lastFormatException.getMessage().isBlank()) {
            errorMessage = "[TCP][FormatError] " + lastFormatException.getMessage();
        }
        System.out.println(errorMessage);
        return errorMessage;
    }
}
