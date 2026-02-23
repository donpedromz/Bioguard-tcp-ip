package com.donpedromz.format.parser;

import com.donpedromz.exceptions.InvalidFastaFormatException;
import com.donpedromz.model.Disease;
import com.donpedromz.model.InfectiousnessLevel;
/**
 * @version 1.0
 * @author juanp
 * Implementación de IContentParser para mensajes FASTA específicos de enfermedades.
 */
public class FastaDiseaseParser implements IContentParser<Disease> {
    /**
     * Formato esperado del mensaje FASTA para enfermedades:
     * - Línea 1: >DiseaseName|DiseaseInfectiousnessLevel(ALTA|MEDIA|BAJA)
     * - Línea 2: Secuencia de la enfermedad (string no vacío)
     */
    private static final int EXPECTED_LINES = 2;
    /**
     * Formato esperado del encabezado en la primera línea del mensaje FASTA para enfermedades:
     * >DiseaseName|DiseaseInfectiousnessLevel(ALTA|MEDIA|BAJA)
     */
    private static final int EXPECTED_HEADER_FIELDS = 2;
    /**
     * Verifica que el mensaje FASTA para enfermedades cumpla con el
     * formato esperado y extrae los datos necesarios para crear un objeto Disease.
     * @param message El mensaje FASTA que se va a verificar y procesar.
     *                Debe tener exactamente 2 líneas, donde la primera línea es el encabezado con el formato
     *                ">DiseaseName|DiseaseInfectiousnessLevel(ALTA|MEDIA|BAJA)"
     *                y la segunda línea es la secuencia de la enfermedad. No debe ser null ni vacío.
     * @return  Un objeto Disease creado a partir de los datos extraídos del mensaje FASTA si el formato es válido.
     */
    private Disease verify(String message) {
        if (message == null || message.isBlank()) {
            throw new InvalidFastaFormatException("FASTA message for Disease cannot be empty");
        }
        if(!message.startsWith(">")){
            throw new InvalidFastaFormatException("Invalid FASTA format for Disease");
        }
        String[] lines = message.trim().split("\\R");
        if(lines.length != EXPECTED_LINES){
            throw new InvalidFastaFormatException("FASTA format for Disease should have exactly " + EXPECTED_LINES + " lines");
        }
        String[] header = lines[0].split("\\|");
        if(header.length != EXPECTED_HEADER_FIELDS){
            throw new InvalidFastaFormatException("FASTA header for Disease should have exactly " + EXPECTED_HEADER_FIELDS + " fields separated by '|'");
        }
        String diseaseName = header[0].substring(1).trim();
        String infectiousnessRaw = header[1].trim();
        InfectiousnessLevel infectiousnessLevel = InfectiousnessLevel.from(infectiousnessRaw);
        String sequence = lines[1].trim();
        return new Disease(
                diseaseName,
                infectiousnessLevel,
                sequence
        );
    }

    @Override
    public Disease parse(String body) {
        return verify(body);
    }
}
