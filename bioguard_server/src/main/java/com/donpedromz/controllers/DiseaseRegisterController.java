package com.donpedromz.controllers;

import com.donpedromz.format.factory.ParserFactory;
import com.donpedromz.exceptions.DataValidationException;
import com.donpedromz.exceptions.InvalidMessageFormatException;
import com.donpedromz.service.IDiseaseService;
import com.donpedromz.model.Disease;
import com.donpedromz.infraestructure.network.data.Request;
import com.donpedromz.infraestructure.network.data.Response;

/**
 * @version 1.0
 * @author juanp
 * Controlador que procesa solicitudes de registro de enfermedades en formato FASTA.
 * Delega la lógica de negocio al servicio {@link IDiseaseService} y se encarga
 * de parsear la solicitud, manejar todas las excepciones de negocio y construir
 * respuestas con formato estandarizado {@code [TCP][STATUS][Category] msg}.
 */
public class DiseaseRegisterController implements IMessageProcessor {
    /**
     * Mensaje genérico para errores internos del servidor (no expone detalles).
     */
    private static final String INTERNAL_ERROR_MESSAGE = "[TCP][500][InternalError] Error interno del servidor";
    /**
     * Prefijo para mensajes de creación exitosa.
     */
    private static final String CREATED_PREFIX = "[TCP][201][Created] ";
    /**
     * Servicio de enfermedades que contiene la lógica de negocio.
     */
    private final IDiseaseService diseaseService;
    /**
     * Fábrica de parsers para convertir el cuerpo de la solicitud en un objeto Disease.
     */
    private final ParserFactory<Disease> diseaseParserFactory;

    /**
     * Construye el controlador de registro de enfermedades.
     * @param diseaseService servicio de enfermedades que aplica las reglas de negocio
     * @param diseaseParserFactory fábrica de parsers para el contenido del mensaje
     */
    public DiseaseRegisterController(IDiseaseService diseaseService, ParserFactory<Disease> diseaseParserFactory) {
        if (diseaseService == null) {
            throw new IllegalArgumentException("diseaseService no puede ser null");
        }
        if (diseaseParserFactory == null) {
            throw new IllegalArgumentException("diseaseParserFactory no puede ser null");
        }
        this.diseaseService = diseaseService;
        this.diseaseParserFactory = diseaseParserFactory;
    }

    /**
     * Procesa la solicitud FASTA para registro de enfermedades.
     * Maneja todas las excepciones de negocio y retorna respuestas con formato TCP estandarizado.
     * @param request solicitud con el contenido FASTA de la enfermedad
     * @return respuesta TCP con el resultado del registro o la causa de error
     */
    @Override
    public Response process(Request request) {
        try {
            Disease disease = diseaseParserFactory.parse(request.getContentType(), request.getBody());
            Disease savedDisease = diseaseService.register(disease);
            String successMessage = CREATED_PREFIX + "virus " + savedDisease.getDiseaseName() + " registrado exitosamente";
            System.out.println(successMessage);
            return new Response.Builder()
                    .statusCode(201)
                    .message(successMessage)
                    .build();
        } catch (InvalidMessageFormatException formatException) {
            String errorMessage = formatException.toTcpMessage();
            System.out.println(errorMessage);
            return new Response.Builder()
                    .statusCode(400)
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
