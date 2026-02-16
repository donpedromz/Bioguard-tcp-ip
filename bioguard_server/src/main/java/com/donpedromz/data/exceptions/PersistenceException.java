package com.donpedromz.data.exceptions;

import com.donpedromz.business.DataValidationException;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando ocurre un error durante la persistencia de datos.
 * Esta excepción indica que se ha producido un problema al intentar guardar o recuperar datos en el sistema,
 * como por ejemplo, una falla en la conexión a la base de datos o un error al escribir en un archivo de diagnóstico.
 */
public class PersistenceException extends DataValidationException {
    public PersistenceException(String message) {
        super(message);
    }

    /**
     * Crea una nueva instancia de PersistenceException con un mensaje de error específico y una causa subyacente.
     * @param message El mensaje de error que describe el problema de persistencia. No debe ser null ni vacío.
     * @param cause La causa subyacente que llevó a la excepción de persistencia. No debe ser null.
     */
    public PersistenceException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
