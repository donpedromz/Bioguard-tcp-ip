package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando se detecta que los datos están corruptos durante la validación.
 * Esta excepción indica que los datos proporcionados no cumplen con las reglas de validación establecidas debido a
 * corrupción, como por ejemplo, un archivo de diagnóstico que no se puede leer correctamente
 * o que contiene información dañada.
 */
public class CorruptedDataException extends DataValidationException {
    private static final String TCP_PREFIX = "[TCP][500][InternalError] ";
    private static final String GENERIC_MESSAGE = "Error interno del servidor";

    public CorruptedDataException(String message) {
        super(message);
    }

    public CorruptedDataException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    /**
     * Devuelve un mensaje genérico para no exponer detalles internos al cliente.
     * @return mensaje con formato {@code [TCP][500][InternalError] Error interno del servidor}
     */
    @Override
    public String toTcpMessage() {
        return TCP_PREFIX + GENERIC_MESSAGE;
    }
}
