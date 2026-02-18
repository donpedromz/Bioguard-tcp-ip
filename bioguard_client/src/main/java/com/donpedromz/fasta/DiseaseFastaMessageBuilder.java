package com.donpedromz.fasta;

import com.donpedromz.domain.disease.DiseaseRegistration;

import java.util.Locale;

/**
 * @version 1.0
 * @author juanp
 * Implementación de FastaMessageBuilder para construir mensajes FASTA a partir de registros de enfermedades,
 * formateando el encabezado con el nombre de la enfermedad y su nivel de contagiosidad,
 * y asegurándose de que la secuencia genética esté en mayúsculas y sin espacios.
 */
public class DiseaseFastaMessageBuilder implements MessageBuilder<DiseaseRegistration> {
    /**
     * Plantilla para el encabezado FASTA,
     * donde %s se reemplazará por el nombre de la enfermedad y su nivel de contagiosidad.
     */
    private static final String HEADER_TEMPLATE = ">%s|%s";

    /**
     * {@inheritDoc}
     * @param payload
     * @return
     */
    @Override
    public String build(DiseaseRegistration payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Disease payload is required");
        }
        String header = HEADER_TEMPLATE.formatted(payload.getName(), payload.getInfectiousnessLevel());
        String sequence = payload.getSequence().toUpperCase(Locale.ROOT);
        return header + System.lineSeparator() + sequence;
    }
}
