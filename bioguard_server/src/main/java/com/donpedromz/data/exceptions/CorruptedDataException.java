package com.donpedromz.data.exceptions;

import com.donpedromz.business.DataValidationException;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando se detecta que los datos están corruptos durante la validación.
 * Esta excepción indica que los datos proporcionados no cumplen con las reglas de validación establecidas debido a
 * corrupción, como por ejemplo, un archivo de diagnóstico que no se puede leer correctamente
 * o que contiene información dañada.
 */
public class CorruptedDataException extends DataValidationException {
    public CorruptedDataException(String message) {
        super(message);
    }

    public CorruptedDataException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
