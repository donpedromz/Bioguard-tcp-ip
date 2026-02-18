package com.donpedromz.service;

import com.donpedromz.domain.diagnostic.DiagnosticRegistration;
import com.donpedromz.domain.disease.DiseaseRegistration;
import com.donpedromz.domain.patient.PatientRegistration;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define el contrato para un cliente de registro que se comunica con un servidor para
 * registrar pacientes, enfermedades y generar diagnósticos.
 */
public interface RegistrationClient {
    /**
     * Envía una solicitud de registro de paciente al servidor.
     * @param patient registro del paciente que se desea enviar al servidor,
     *                generalmente contiene información personal y médica relevante para el proceso de registro.
     * @return un String que representa la respuesta textual
     * devuelta por el servidor.
     */
    String registerPatient(PatientRegistration patient);

    /**
     * Envía una solicitud de registro de enfermedad al servidor.
     * @param disease registro de la enfermedad que se desea enviar al servidor.
     * @return un String que representa la respuesta textual devuelta por el servidor.
     */
    String registerDisease(DiseaseRegistration disease);

    /**
     * Envía una solicitud para generar un diagnóstico al servidor,
     * utilizando un mensaje en formato FASTA que contiene información relevante sobre el paciente y sus síntomas.
     * @param diagnosticRegistration registro del diagnóstico que se desea enviar al servidor.
     * @return un String que representa la respuesta textual devuelta por el servidor.
     */
    String generateDiagnostic(DiagnosticRegistration diagnosticRegistration);
}
