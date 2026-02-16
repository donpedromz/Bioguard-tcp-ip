package com.donpedromz.business.FASTA.exceptions;

public class InvalidFastaFormatException extends InvalidMessageFormatException {
    public InvalidFastaFormatException(String message) {
        super(message);
    }
    public InvalidFastaFormatException(){
        super("Invalid FASTA format. Unable to process the message.");
    }
}
