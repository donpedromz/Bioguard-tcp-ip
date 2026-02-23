package com.donpedromz.service;

import com.donpedromz.domain.diagnostic.DiagnosticRegistration;
import com.donpedromz.domain.disease.DiseaseRegistration;
import com.donpedromz.domain.patient.PatientRegistration;
import com.donpedromz.fasta.MessageBuilder;
import com.donpedromz.network.RequestBuilder;
import com.donpedromz.network.TCPClient;

/**
 * @version 2.0
 * @author juanp
 * Implementación de RegistrationClient que utiliza un TCPClient para enviar mensajes al servidor.
 * Construye los encabezados de protocolo (método, acción, content-type) requeridos por el router del servidor
 * antes de enviar el cuerpo FASTA generado por los MessageBuilders.
 */
public class TCPRegistrationClient implements RegistrationClient {
    /**
     * Content-type para mensajes en formato FASTA.
     */
    private static final String FASTA_CONTENT_TYPE = "application/fasta";
    /**
     * Método HTTP-like para solicitudes de creación/registro.
     */
    private static final String POST_METHOD = "POST";
    /**
     * Cliente TCP utilizado para enviar mensajes al servidor.
     */
    private final TCPClient tcpClient;
    /**
     * Builder que transforma registros de pacientes en cuerpo FASTA.
     */
    private final MessageBuilder<PatientRegistration> patientBuilder;
    /**
     * Builder que transforma registros de enfermedades en cuerpo FASTA.
     */
    private final MessageBuilder<DiseaseRegistration> diseaseBuilder;
    /**
     * Builder que transforma registros de diagnósticos en cuerpo FASTA.
     */
    private final MessageBuilder<DiagnosticRegistration> diagnosticBuilder;

    /**
     * Constructor que inyecta las dependencias necesarias para enviar mensajes al servidor.
     * @param tcpClient cliente TCP que se utilizará para enviar mensajes al servidor
     * @param patientBuilder builder que convierte registros de pacientes en cuerpo FASTA
     * @param diseaseBuilder builder que convierte registros de enfermedades en cuerpo FASTA
     * @param diagnosticBuilder builder que convierte registros de diagnósticos en cuerpo FASTA
     */
    public TCPRegistrationClient(
            TCPClient tcpClient,
            MessageBuilder<PatientRegistration> patientBuilder,
            MessageBuilder<DiseaseRegistration> diseaseBuilder,
            MessageBuilder<DiagnosticRegistration> diagnosticBuilder
    ) {
        this.tcpClient = tcpClient;
        this.patientBuilder = patientBuilder;
        this.diseaseBuilder = diseaseBuilder;
        this.diagnosticBuilder = diagnosticBuilder;
    }

    /**
     * Envía una solicitud de registro de paciente al servidor.
     * Construye un mensaje TCP con encabezados {@code POST patient} y content-type {@code application/fasta}.
     * @param patient registro del paciente que se desea enviar al servidor
     * @return respuesta textual devuelta por el servidor
     */
    @Override
    public String registerPatient(PatientRegistration patient) {
        String fastaBody = patientBuilder.build(patient);
        String payload = new RequestBuilder()
                .method(POST_METHOD)
                .action("patient")
                .contentType(FASTA_CONTENT_TYPE)
                .body(fastaBody)
                .build();
        return tcpClient.send(payload);
    }

    /**
     * Envía una solicitud de registro de enfermedad al servidor.
     * Construye un mensaje TCP con encabezados {@code POST disease} y content-type {@code application/fasta}.
     * @param disease registro de la enfermedad que se desea enviar al servidor
     * @return respuesta textual devuelta por el servidor
     */
    @Override
    public String registerDisease(DiseaseRegistration disease) {
        String fastaBody = diseaseBuilder.build(disease);
        String payload = new RequestBuilder()
                .method(POST_METHOD)
                .action("disease")
                .contentType(FASTA_CONTENT_TYPE)
                .body(fastaBody)
                .build();
        return tcpClient.send(payload);
    }

    /**
     * Envía una solicitud para generar un diagnóstico al servidor.
     * Construye un mensaje TCP con encabezados {@code POST diagnose} y content-type {@code application/fasta}.
     * @param diagnosticRegistration registro del diagnóstico que se desea enviar al servidor
     * @return respuesta textual devuelta por el servidor
     */
    @Override
    public String generateDiagnostic(DiagnosticRegistration diagnosticRegistration) {
        String fastaBody = diagnosticBuilder.build(diagnosticRegistration);
        String payload = new RequestBuilder()
                .method(POST_METHOD)
                .action("diagnose")
                .contentType(FASTA_CONTENT_TYPE)
                .body(fastaBody)
                .build();
        return tcpClient.send(payload);
    }
}
