package com.donpedromz.fasta;


import com.donpedromz.domain.diagnostic.DiagnosticRegistration;

import java.util.Locale;

/**
 * @version 1.0
 * @author juanp
 * Implementación de FastaMessageBuilder para construir mensajes FASTA a partir de registros de diagnóstico,
 * formateando el encabezado con el documento del paciente y la fecha de la muestra,
 * y asegurándose de que la secuencia genética esté en mayúsculas y sin espacios.
 */
public class DiagnosticFastaMessageBuilder implements MessageBuilder<DiagnosticRegistration> {
    /**
     * Plantilla para el encabezado FASTA,
     * donde %s se reemplazará por el documento del paciente y la fecha de la muestra.
     */
    private static final String HEADER_TEMPLATE = ">%s|%s";

    /**
     * {@inheritDoc}
     * @param payload
     * @return
     */
    @Override
    public String build(DiagnosticRegistration payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Diagnostic payload is required");
        }
        String header = HEADER_TEMPLATE.formatted(payload.getPatientDocument(), payload.getSampleDate());
        String sequence = payload.getGeneticSequence().toUpperCase(Locale.ROOT);
        return header + System.lineSeparator() + sequence;
    }
}
