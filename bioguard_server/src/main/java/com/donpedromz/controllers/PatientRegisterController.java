package com.donpedromz.controllers;

import com.donpedromz.format.factory.ParserFactory;
import com.donpedromz.exceptions.DataValidationException;
import com.donpedromz.exceptions.InvalidMessageFormatException;
import com.donpedromz.service.IPatientService;
import com.donpedromz.model.Patient;
import com.donpedromz.infrastructure.network.data.Request;
import com.donpedromz.infrastructure.network.data.Response;

/**
 * @version 1.0
 * @author juanp
 * Controlador que procesa solicitudes de registro de pacientes en formato FASTA.
 * Delega la lógica de negocio al servicio {@link IPatientService} y se encarga
 * de parsear la solicitud, manejar todas las excepciones de negocio y construir
 * respuestas con formato estandarizado {@code [TCP][STATUS][Category] msg}.
 */
public class PatientRegisterController implements IMessageProcessor {
    /**
     * Mensaje genérico para errores internos del servidor (no expone detalles).
     */
    private static final String INTERNAL_ERROR_MESSAGE = "[TCP][500][InternalError] Error interno del servidor";
    /**
     * Prefijo para mensajes de creación exitosa.
     */
    private static final String CREATED_PREFIX = "[TCP][201][Created] ";
    /**
     * Servicio de pacientes que contiene la lógica de negocio.
     */
    private final IPatientService patientService;
    /**
     * Fábrica de parsers para convertir el cuerpo de la solicitud en un objeto Patient.
     */
    private final ParserFactory<Patient> patientParserFactory;

    /**
     * Construye el controlador de registro de pacientes.
     * @param patientService servicio de pacientes que aplica las reglas de negocio
     * @param patientParserFactory fábrica de parsers para el contenido del mensaje
     */
    public PatientRegisterController(IPatientService patientService, ParserFactory<Patient> patientParserFactory) {
        if (patientService == null) {
            throw new IllegalArgumentException("patientService no puede ser null");
        }
        if (patientParserFactory == null) {
            throw new IllegalArgumentException("patientParserFactory no puede ser null");
        }
        this.patientService = patientService;
        this.patientParserFactory = patientParserFactory;
    }

    /**
     * Procesa la solicitud FASTA para registro de pacientes.
     * Maneja todas las excepciones de negocio y retorna respuestas con formato TCP estandarizado.
     * @param request solicitud con el contenido FASTA del paciente
     * @return respuesta TCP con el resultado del registro o la causa de error
     */
    @Override
    public Response process(Request request) {
        try {
            Patient patient = patientParserFactory.parse(request.getContentType(), request.getBody());
            Patient savedPatient = patientService.register(patient);
            String successMessage = CREATED_PREFIX + "paciente registrado exitosamente con uuid: " + savedPatient.getUuid();
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
