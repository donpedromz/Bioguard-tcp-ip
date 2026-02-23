package com.donpedromz.exceptions;

public class InvalidFastaFormatException extends InvalidMessageFormatException {
    public InvalidFastaFormatException(String message) {
        super(message);
    }
    public InvalidFastaFormatException(){
        super("Formato FASTA invalido. No se puede procesar la solicitud.");
    }
}
