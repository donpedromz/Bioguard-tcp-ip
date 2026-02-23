package com.donpedromz.controllers;

import com.donpedromz.format.factory.ParserFactory;
import com.donpedromz.exceptions.DataValidationException;
import com.donpedromz.exceptions.InvalidMessageFormatException;
import com.donpedromz.dtos.DiagnoseMessageDto;
import com.donpedromz.service.IDiagnoseService;
import com.donpedromz.dtos.DiagnoseResult;
import com.donpedromz.exceptions.ConflictException;
import com.donpedromz.exceptions.CorruptedDataException;
import com.donpedromz.exceptions.NotFoundException;
import com.donpedromz.infrastructure.network.data.Request;
import com.donpedromz.infrastructure.network.data.Response;

/**
 * @version 1.0
 * @author juanp
 * Controlador que procesa solicitudes de diagnóstico a partir de muestras genéticas FASTA.
 * Delega la lógica de negocio al servicio {@link IDiagnoseService} y se encarga
 * de parsear la solicitud, manejar todas las excepciones de negocio y construir
 * respuestas con formato estandarizado {@code [TCP][STATUS][Category] msg}.
 */
public class DiagnoseController implements IMessageProcessor {
    /**
     * Mensaje genérico para errores internos del servidor (no expone detalles).
     */
    private static final String INTERNAL_ERROR_MESSAGE = "[TCP][500][InternalError] Error interno del servidor";
    /**
     * Prefijo para mensajes de éxito en diagnósticos.
     */
    private static final String SUCCESS_PREFIX = "[TCP][200][Success] ";
    /**
     * Separador utilizado para concatenar mensajes de operación adicionales.
     */
    private static final String MESSAGE_SEPARATOR = " | ";
    /**
     * Servicio de diagnóstico que contiene la lógica de negocio.
     */
    private final IDiagnoseService diagnoseService;
    /**
     * Fábrica de parsers para convertir el cuerpo de la solicitud en un DTO de diagnóstico.
     */
    private final ParserFactory<DiagnoseMessageDto> diagnosticParserFactory;

    /**
     * Construye el controlador de diagnóstico FASTA.
     * @param diagnoseService servicio de diagnóstico que aplica las reglas de negocio
     * @param diagnosticParserFactory fábrica de parsers para el contenido del mensaje
     */
    public DiagnoseController(
            IDiagnoseService diagnoseService,
            ParserFactory<DiagnoseMessageDto> diagnosticParserFactory
    ) {
        if (diagnoseService == null) {
            throw new IllegalArgumentException("diagnoseService no puede ser null");
        }
        if (diagnosticParserFactory == null) {
            throw new IllegalArgumentException("diagnosticParserFactory no puede ser null");
        }
        this.diagnoseService = diagnoseService;
        this.diagnosticParserFactory = diagnosticParserFactory;
    }

    /**
     * Procesa la solicitud FASTA y genera un diagnóstico delegando al servicio.
     * Maneja todas las excepciones de negocio y retorna respuestas con formato TCP estandarizado.
     * @param request solicitud FASTA de entrada
     * @return respuesta TCP con el resultado o la causa de rechazo
     */
    @Override
    public Response process(Request request) {
        try {
            DiagnoseMessageDto diagnoseMessage = diagnosticParserFactory.parse(
                    request.getContentType(), request.getBody());

            DiagnoseResult result = diagnoseService.diagnose(diagnoseMessage);

            String responseMessage = SUCCESS_PREFIX + "diagnostico generado exitosamente";
            if (!result.getOperationMessages().isEmpty()) {
                responseMessage = responseMessage + MESSAGE_SEPARATOR
                        + String.join(MESSAGE_SEPARATOR, result.getOperationMessages());
            }
            System.out.println(responseMessage);
            return new Response.Builder()
                    .statusCode(200)
                    .message(responseMessage)
                    .build();
        } catch (InvalidMessageFormatException formatException) {
            String errorMessage = formatException.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(400)
                    .message(errorMessage)
                    .build();
        } catch (ConflictException conflictException) {
            String errorMessage = conflictException.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(409)
                    .message(errorMessage)
                    .build();
        } catch (CorruptedDataException corruptedException) {
            String errorMessage = corruptedException.toTcpMessage();
            System.out.println("[SERVER][CorruptedData] " + corruptedException.getMessage());
            return new Response.Builder()
                    .statusCode(500)
                    .message(errorMessage)
                    .build();
        } catch (NotFoundException notFoundException) {
            String errorMessage = notFoundException.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(404)
                    .message(errorMessage)
                    .build();
        } catch (DataValidationException validationException) {
            String errorMessage = validationException.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(400)
                    .message(errorMessage)
                    .build();
        } catch (Exception e) {
            System.out.println("[SERVER][UnexpectedError] " + e.getMessage());
            return new Response.Builder()
                    .statusCode(500)
                    .message(INTERNAL_ERROR_MESSAGE)
                    .build();
        }
    }
}
