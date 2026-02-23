package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando se detecta un conflicto de datos durante la validación. Esta excepción indica que los datos proporcionados entran en conflicto con las reglas de validación establecidas,
 * como por ejemplo, intentar guardar un diagnóstico con un identificador que ya existe en el sistema.
 */
public class ConflictException extends DataValidationException {
    private static final String TCP_PREFIX = "[TCP][409][Conflict] ";
    /**
     * Crea una nueva instancia de ConflictException con un mensaje de error específico.
     * @param message El mensaje de error que describe el conflicto de datos. No debe ser null ni vacío.
     */
    public ConflictException(String message) {
        super(message);
    }

    /**
     * Convierte el mensaje de error en formato TCP estandarizado para conflictos.
     * @return mensaje con formato {@code [TCP][409][Conflict] msg}
     */
    @Override
    public String toTcpMessage() {
        return TCP_PREFIX + getMessage();
    }
}
