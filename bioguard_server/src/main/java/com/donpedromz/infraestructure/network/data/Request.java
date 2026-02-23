package com.donpedromz.infraestructure.network.data;
/**
 * @version 1.0
 * @author juanp
 * Clase que representa una solicitud TCP recibida por el servidor, encapsulando los componentes clave del mensaje.
 * Contiene el método (e.g., GET, POST), la acción o ruta solicitada, el tipo de contenido (e.g., application/json) y el cuerpo del mensaje.
 */
public class Request {
    private String method;
    private String action;
    private String contentType;
    private String body;
    public Request(String method, String action, String contentType, String[] headers, String body) {
        this.method = method;
        this.action = action;
        this.contentType = contentType;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
