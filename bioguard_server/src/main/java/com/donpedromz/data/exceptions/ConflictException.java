package com.donpedromz.data.exceptions;

import com.donpedromz.business.exceptions.DataValidationException;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando se detecta un conflicto de datos durante la validación. Esta excepción indica que los datos proporcionados entran en conflicto con las reglas de validación establecidas,
 * como por ejemplo, intentar guardar un diagnóstico con un identificador que ya existe en el sistema.
 */
public class ConflictException extends DataValidationException {
    /**
     * Crea una nueva instancia de ConflictException con un mensaje de error específico.
     * @param message El mensaje de error que describe el conflicto de datos. No debe ser null ni vacío.
     */
    public ConflictException(String message) {
        super(message);
    }
}
