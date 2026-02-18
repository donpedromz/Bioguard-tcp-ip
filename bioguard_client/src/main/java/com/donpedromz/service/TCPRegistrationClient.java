package com.donpedromz.service;

import com.donpedromz.domain.diagnostic.DiagnosticRegistration;
import com.donpedromz.domain.disease.DiseaseRegistration;
import com.donpedromz.domain.patient.PatientRegistration;
import com.donpedromz.fasta.MessageBuilder;
import com.donpedromz.network.TCPClient;

/**
 * @version 1.0
 * @author juanp
 * Implementación de RegistrationClient que utiliza un TCPClient para enviar mensajes al servidor.
 */
public class TCPRegistrationClient implements RegistrationClient {
    /**
     * Cliente TCP utilizado para enviar mensajes al servidor.
     */
    private final TCPClient tcpClient;
    /**
     * Builder que transforma registros de pacientes en mensajes FASTA.
     */
    private final MessageBuilder<PatientRegistration> patientBuilder;
    /**
     * Builder que transforma registros de enfermedades en mensajes FASTA.
     */
    private final MessageBuilder<DiseaseRegistration> diseaseBuilder;
    /**
     * Builder que transforma registros de diagnósticos en mensajes FASTA.
     */
    private final MessageBuilder<DiagnosticRegistration> diagnosticBuilder;

    /**
     * Constructor que inyecta las dependencias necesarias para enviar mensajes al servidor.
     * @param tcpClient cliente TCP que se utilizará para enviar mensajes al servidor,
     * @param patientBuilder builder que se encargará de convertir los registros de pacientes en mensajes FASTA,
     * @param diseaseBuilder builder que se encargará de convertir los registros de enfermedades en mensajes FASTA.
     * @param diagnosticBuilder builder que se encargará de convertir los registros de diagnósticos en mensajes FASTA.
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
     * Envía una solicitud de registro de paciente al servidor, utilizando el builder configurado para
     * convertir el registro del paciente en un mensaje FASTA antes de enviarlo.
     * @param patient registro del paciente que se desea enviar al servidor,
     *                generalmente contiene información personal y médica relevante para el proceso de registro.
     * @return un String que representa la respuesta textual
     * devuelta por el servidor después de procesar la solicitud de registro del paciente.
     */
    @Override
    public String registerPatient(PatientRegistration patient) {
        String payload = patientBuilder.build(patient);
        return tcpClient.send(payload);
    }

    /**
     * Envía una solicitud de registro de enfermedad al servidor, utilizando el builder configurado para
     * convertir el registro de la enfermedad en un mensaje FASTA antes de enviarlo.
     * @param disease registro de la enfermedad que se desea enviar al servidor.
     * @return un String que representa la respuesta textual devuelta
     * por el servidor después de procesar la solicitud de registro de la enfermedad.
     */
    @Override
    public String registerDisease(DiseaseRegistration disease) {
        String payload = diseaseBuilder.build(disease);
        return tcpClient.send(payload);
    }

    /**
     * Envía una solicitud para generar un diagnóstico al servidor, utilizando el builder configurado para
     * convertir el registro del diagnóstico en un mensaje FASTA antes de enviarlo.
     * @param diagnosticRegistration registro del diagnóstico que se desea enviar al servidor.
     * @return un String que representa la respuesta textual devuelta por
     * el servidor después de procesar la solicitud para generar un diagnóstico.
     */
    @Override
    public String generateDiagnostic(DiagnosticRegistration diagnosticRegistration) {
        String payload = diagnosticBuilder.build(diagnosticRegistration);
        return tcpClient.send(payload);
    }
}
