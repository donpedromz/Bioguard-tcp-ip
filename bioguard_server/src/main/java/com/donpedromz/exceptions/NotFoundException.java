package com.donpedromz.exceptions;

/**
 * @version 1.0
 * @author juanp
 * Excepción que se lanza cuando no se encuentra un recurso o
 * entidad específica durante las operaciones de almacenamiento o recuperación de datos.
 */
public class NotFoundException extends RuntimeException {
    /**
     * Prefijo utilizado para identificar los mensajes de
     * error relacionados con la falta de recursos en el contexto de TCP.
     */
    private static final String TCP_PREFIX = "[TCP][404][NotFound] ";

    /**
     * Crea una nueva instancia de NotFoundException con un mensaje de error específico.
     * @param message El mensaje de error que describe el
     *                recurso o entidad que no se encontró. No debe ser null ni vacío.
     */
    public NotFoundException(String message) {
        super(message);
    }
    /**
     * Convierte el mensaje de error en formato TCP estandarizado.
     * @return mensaje con formato {@code [TCP][404][NotFound] msg}
     */
    public String toTcpMessage() {
        return TCP_PREFIX + getMessage();
    }
}
