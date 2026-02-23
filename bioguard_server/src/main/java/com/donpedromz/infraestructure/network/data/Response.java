package com.donpedromz.infraestructure.network.data;
/**
 * @version 1.0
 * @author juanp
 * Clase que representa una respuesta TCP que el servidor enviará al cliente, encapsulando el código de estado, un mensaje descriptivo y un cuerpo opcional con datos adicionales.
 * Utiliza el patrón Builder para facilitar la construcción de respuestas con diferentes combinaciones de campos.
 */
public class Response {
    private final int statusCode;
    private final String message;
    private final String body;

    private Response(Builder builder) {
        statusCode = builder.statusCode;
        message = builder.message;
        body = builder.body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (message != null) {
            sb.append(message);
        }
        if (body != null) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(body);
        }
        return sb.toString();
    }

    /**
     * {@code Response} builder static inner class.
     */
    public static final class Builder {
        private int statusCode;
        private String message;
        private String body;

        public Builder() {
        }
        /**
         * Sets the {@code statusCode} and returns a reference to this Builder enabling method chaining.
         *
         * @param val the {@code statusCode} to set
         * @return a reference to this Builder
         */
        public Builder statusCode(int val) {
            statusCode = val;
            return this;
        }
        /**
         * Sets the {@code message} and returns a reference to this Builder enabling method chaining.
         *
         * @param val the {@code message} to set
         * @return a reference to this Builder
         */
        public Builder message(String val) {
            message = val;
            return this;
        }

        /**
         * Sets the {@code body} and returns a reference to this Builder enabling method chaining.
         *
         * @param val the {@code body} to set
         * @return a reference to this Builder
         */
        public Builder body(String val) {
            body = val;
            return this;
        }

        /**
         * Returns a {@code Response} built from the parameters previously set.
         *
         * @return a {@code Response} built with parameters of this {@code Response.Builder}
         */
        public Response build() {
            return new Response(this);
        }
    }
}
