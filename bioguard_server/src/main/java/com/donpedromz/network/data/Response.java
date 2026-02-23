package com.donpedromz.network.data;

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
        sb.append(statusCode);
        if (message != null) {
            sb.append("\n").append(message);
        }
        if (body != null) {
            sb.append("\n").append(body);
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
