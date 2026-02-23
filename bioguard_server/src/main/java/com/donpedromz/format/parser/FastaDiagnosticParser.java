package com.donpedromz.format.parser;

import com.donpedromz.format.dto.DiagnoseMessageDto;
import com.donpedromz.exceptions.InvalidFastaFormatException;

import java.util.Locale;
/**
 * @version 1.0
 * @author juanp
 * Implementación de IContentParser para mensajes FASTA específicos de diagnóstico.
 */
public class FastaDiagnosticParser implements IContentParser<DiagnoseMessageDto>{
    /**
     * Constantes para validación de formato FASTA específico de diagnóstico:
     * - Se esperan exactamente 2 líneas: una de encabezado y una de secuencia genética:
     * >documento|fecha
     * SECUENCIA_GENÉTICA
     */
    private static final int EXPECTED_LINES = 2;
    /**
     * El encabezado debe contener exactamente 2 campos:
     * >documento|fecha
     */
    private static final int EXPECTED_HEADER_FIELDS = 2;
    /**
     * La fecha en el encabezado debe seguir el formato YYYY-MM-DD, validado con esta expresión regular.
     */
    private static final String DATE_FORMAT_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
    /**
     * Valida y normaliza el mensaje FASTA de diagnóstico.
     * @param message mensaje FASTA bruto recibido por TCP
     * @return DTO con documento, fecha, secuencia y mensaje original normalizado
     */
    private DiagnoseMessageDto verify(String message) {
        if (message == null || message.isBlank()) {
            throw new InvalidFastaFormatException("FASTA message for Diagnose cannot be empty");
        }
        if (!message.startsWith(">")) {
            throw new InvalidFastaFormatException("Invalid FASTA format for Diagnose");
        }
        String[] lines = message.trim().split("\\R");
        if (lines.length != EXPECTED_LINES) {
            throw new InvalidFastaFormatException(
                    "FASTA format for Diagnose should have exactly " + EXPECTED_LINES + " lines");
        }
        String[] header = lines[0].split("\\|");
        if (header.length != EXPECTED_HEADER_FIELDS) {
            throw new InvalidFastaFormatException("FASTA header for Diagnose should have exactly "
                    + EXPECTED_HEADER_FIELDS + " fields separated by '|'");
        }
        String patientDocument = header[0].substring(1).trim();
        String sampleDate = header[1].trim();
        if (!sampleDate.matches(DATE_FORMAT_REGEX)) {
            throw new InvalidFastaFormatException(
                    "FASTA header for Diagnose requires a date in YYYY-MM-DD format as second field");
        }
        String geneticSequence = lines[1].trim().toUpperCase(Locale.ROOT);

        return new DiagnoseMessageDto(
                patientDocument,
                sampleDate,
                geneticSequence
        );
    }

    @Override
    public DiagnoseMessageDto parse(String body) {
        return verify(body);
    }
}
