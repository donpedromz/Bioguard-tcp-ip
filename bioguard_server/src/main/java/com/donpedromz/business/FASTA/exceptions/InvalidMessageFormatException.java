package com.donpedromz.business.FASTA.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción personalizada para indicar que el formato del mensaje TCP recibido es inválido y no se puede procesar.
 */
public class InvalidMessageFormatException extends RuntimeException {
    public InvalidMessageFormatException(String message) {
        super(message);
    }
    public InvalidMessageFormatException(){
        super("Invalid message format. Unable to process the message.");
    }
}
