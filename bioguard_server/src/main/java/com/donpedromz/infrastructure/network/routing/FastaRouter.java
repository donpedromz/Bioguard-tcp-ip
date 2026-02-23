package com.donpedromz.infrastructure.network.routing;

import java.util.HashMap;
import java.util.Map;

import com.donpedromz.controllers.IMessageProcessor;
import com.donpedromz.exceptions.MalformedRequestException;
import com.donpedromz.exceptions.RouteNotFoundException;
import com.donpedromz.infrastructure.network.data.Request;
import com.donpedromz.infrastructure.network.data.Response;

/**
 * @version 2.0
 * @author juanp
 * Router que parsea mensajes TCP en formato FASTA (3 líneas: request-line, content-type, body)
 * y los despacha al controlador correspondiente según la tabla de enrutamiento.
 * Maneja errores de formato de mensaje y rutas no encontradas, retornando respuestas
 * con formato estandarizado {@code [TCP][STATUS][Category] msg}.
 */
public class FastaRouter extends MessageRouter {
    private static final int REQUEST_LINE_INDEX = 0;
    private static final int CONTENT_TYPE_INDEX = 1;
    private static final int BODY_INDEX = 2;
    private static final int EXPECTED_PARTS = 3;
    private static final int METHOD_INDEX = 0;
    private static final int ACTION_INDEX = 1;
    private static final int EXPECTED_REQUEST_LINE_PARTS = 2;

    /**
     * Mensaje genérico para errores internos del servidor (no expone detalles).
     */
    private static final String INTERNAL_ERROR_MESSAGE = "[TCP][500][InternalError] Error interno del servidor";

    public FastaRouter() {
        super(new HashMap<>());
    }

    public FastaRouter(Map<String, IMessageProcessor> routingTable) {
        super(routingTable);
    }

    /**
     * Parsea el mensaje TCP en sus componentes (request-line, content-type, body),
     * busca el procesador correspondiente en la tabla de enrutamiento y delega el procesamiento.
     * Maneja errores de formato y rutas no encontradas con respuestas estandarizadas.
     * @param message mensaje TCP completo recibido del cliente
     * @return respuesta TCP generada por el controlador o una respuesta de error
     */
    @Override
    public Response dispatchMessage(String message) {
        try {
            if (message == null || message.isBlank()) {
                throw new MalformedRequestException("El mensaje recibido esta vacio o es nulo");
            }

            String[] messageParts = message.split("\n", EXPECTED_PARTS);
            String[] requestLineParts = getRequestLineParts(messageParts);

            String method = requestLineParts[METHOD_INDEX].trim();
            String action = requestLineParts[ACTION_INDEX].trim();
            String contentType = messageParts[CONTENT_TYPE_INDEX].trim();
            String body = messageParts[BODY_INDEX].trim();

            String routeKey = method + ":" + action;
            IMessageProcessor processor = getProcessor(routeKey);

            Request request = new Request(method, action, contentType, null, body);
            return processor.process(request);
        } catch (MalformedRequestException malformedEx) {
            String errorMessage = malformedEx.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(400)
                    .message(errorMessage)
                    .build();
        } catch (RouteNotFoundException routeEx) {
            String errorMessage = routeEx.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(404)
                    .message(errorMessage)
                    .build();
        } catch (Exception e) {
            System.out.println("[SERVER][RouterError] " + e.getMessage());
            return new Response.Builder()
                    .statusCode(500)
                    .message(INTERNAL_ERROR_MESSAGE)
                    .build();
        }
    }

    private static String[] getRequestLineParts(String[] messageParts) {
        if (messageParts.length < EXPECTED_PARTS) {
            throw new MalformedRequestException(
                    "El mensaje debe contener al menos 3 partes: request-line, content-type y body. Se recibieron "
                            + messageParts.length + " parte(s)");
        }

        String requestLine = messageParts[REQUEST_LINE_INDEX].trim();
        String[] requestLineParts = requestLine.split(" ");
        if (requestLineParts.length < EXPECTED_REQUEST_LINE_PARTS) {
            throw new MalformedRequestException(
                    "La linea de solicitud debe contener metodo y accion separados por espacio. Se recibio: '" + requestLine + "'");
        }
        return requestLineParts;
    }

    /**
     * Obtiene el procesador de mensajes asociado a la clave de ruta dada.
     * @param routeKey clave de enrutamiento con formato {@code METHOD:action}
     * @return procesador de mensajes correspondiente
     * @throws RouteNotFoundException si no existe un procesador para la ruta solicitada
     */
    private IMessageProcessor getProcessor(String routeKey) {
        if (!super.routingTable.containsKey(routeKey)) {
            throw new RouteNotFoundException("No se encontro un procesador para la ruta: " + routeKey);
        }
        return super.routingTable.get(routeKey);
    }
}
