package com.donpedromz.fasta;

import com.donpedromz.domain.patient.PatientRegistration;

/**
 * @version 1.0
 * @author juanp
 * Implementación de FastaMessageBuilder para construir mensajes FASTA a partir de registros de pacientes.
 */
public class PatientFastaMessageBuilder implements MessageBuilder<PatientRegistration> {
    /**
     * Plantilla para el encabezado FASTA,
     * donde cada %s o %d se reemplazará por los campos correspondientes del registro del paciente,
     * formateando el mensaje con el documento de identidad, nombre completo,
     * edad, correo electrónico, género, ciudad y país de residencia.
     */
    private static final String TEMPLATE = ">%s|%s|%s|%d|%s|%s|%s|%s";

    /**
     * {@inheritDoc}
     * @param payload el objeto de tipo T que contiene la información necesaria para construir el mensaje FASTA,
     * @return
     */
    @Override
    public String build(PatientRegistration payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Patient payload is required");
        }
        return TEMPLATE.formatted(
                payload.getDocumentId(),
                payload.getFirstName(),
                payload.getLastName(),
                payload.getAge(),
                payload.getEmail(),
                payload.getGender(),
                payload.getCity(),
                payload.getCountry()
        );
    }
}
