package com.donpedromz.network;

/**
 * @version 1.0
 * @author juanp
 * Construye el mensaje TCP completo que espera el servidor BioGuard,
 * anteponiendo la línea de solicitud (método + acción) y el content-type
 * al cuerpo FASTA generado por los MessageBuilders.
 * <p>
 * Formato resultante (3 líneas separadas por {@code \n}):
 * <pre>
 * METHOD ACTION
 * content-type
 * body
 * </pre>
 * Ejemplo:
 * <pre>
 * POST patient
 * application/fasta
 * &gt;12345|Juan|Perez|25|juan@mail.com|MASCULINO|Bogota|Colombia
 * </pre>
 */
public class RequestBuilder {
    /**
     * Separador de líneas utilizado en el protocolo TCP de BioGuard.
     */
    private static final String LINE_SEPARATOR = "\n";

    private String method;
    private String action;
    private String contentType;
    private String body;

    public RequestBuilder() {
    }

    /**
     * Establece el método HTTP-like de la solicitud (e.g. POST, GET).
     * @param method método de la solicitud
     * @return esta instancia para encadenamiento
     */
    public RequestBuilder method(String method) {
        this.method = method;
        return this;
    }

    /**
     * Establece la acción/recurso de la solicitud (e.g. patient, disease, diagnose).
     * @param action acción de la solicitud
     * @return esta instancia para encadenamiento
     */
    public RequestBuilder action(String action) {
        this.action = action;
        return this;
    }

    /**
     * Establece el content-type del mensaje (e.g. application/fasta).
     * @param contentType tipo de contenido
     * @return esta instancia para encadenamiento
     */
    public RequestBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Establece el cuerpo del mensaje (contenido FASTA).
     * @param body cuerpo del mensaje
     * @return esta instancia para encadenamiento
     */
    public RequestBuilder body(String body) {
        this.body = body;
        return this;
    }

    /**
     * Construye el mensaje TCP completo con el formato esperado por el servidor.
     * @return mensaje TCP listo para enviar
     * @throws IllegalStateException si algún campo obligatorio no fue establecido
     */
    public String build() {
        if (method == null || method.isBlank()) {
            throw new IllegalStateException("method es obligatorio");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalStateException("action es obligatorio");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalStateException("contentType es obligatorio");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("body es obligatorio");
        }
        return method + " " + action + LINE_SEPARATOR
                + contentType + LINE_SEPARATOR
                + body;
    }
}
