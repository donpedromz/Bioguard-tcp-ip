package com.donpedromz.format.parser;

import com.donpedromz.exceptions.InvalidFastaFormatException;
import com.donpedromz.model.Patient;
/**
 * @version 1.0
 * @author juanp
 * Implementación de IContentParser para mensajes FASTA específicos de pacientes. 
 */
public class FastaPatientParser implements IContentParser<Patient>{
    /**
     * Formato esperado del mensaje FASTA para pacientes:
     * - Línea única: >PatientDocument|FirstName|LastName|Age|Email|Gender|City|Country
     */
    private static final int EXPECTED_HEADER_FIELDS = 8;
    @Override
    public Patient parse(String body) {
        return verify(body);
    }
    /**
     * Verifica que el mensaje FASTA para pacientes cumpla con el formato esperado y extrae
     * los datos necesarios para crear un objeto Patient.
     * @param message El mensaje FASTA que se va a verificar y procesar.
     *                Debe tener exactamente 1 línea, donde la línea es el encabezado con el formato
     * @return Un objeto Patient creado a partir de los datos extraídos del mensaje FASTA si el formato es válido.
     */
    private Patient verify(String message) {
        String[] header = getStrings(message);
        String patientDocumentStr = header[0].substring(1).trim();
        String firstName = header[1].trim();
        String lastName = header[2].trim();
        String ageStr = header[3].trim();
        String email = header[4].trim();
        String gender = header[5].trim();
        String city = header[6].trim();
        String country = header[7].trim();
        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            throw new InvalidFastaFormatException("Age field must be numeric");
        }
        return new Patient(
                patientDocumentStr,
                firstName,
                lastName,
                age,
                email,
                gender,
                city,
                country
        );
    }

    /**
     * Extrae los campos del encabezado del mensaje FASTA para pacientes y verifica que cumpla con el formato esperado.
     * @param message El mensaje FASTA que se va a verificar y procesar.
     *                Debe tener exactamente 1 línea, donde la línea es el encabezado con el formato
     * @return Un arreglo de cadenas con los campos extraídos del encabezado del mensaje FASTA si el formato es válido.
     */
    private static String[] getStrings(String message) {
        if (message == null || message.isBlank()) {
            throw new InvalidFastaFormatException("FASTA message for Patient cannot be empty");
        }
        if(!message.startsWith(">")){
            throw new InvalidFastaFormatException("Invalid FASTA format for Patient");
        }
        String[] lines = message.trim().split("\\R");
        if(lines.length != 1){
            throw new InvalidFastaFormatException("FASTA format for Patient should have exactly 1 line");
        }
        String[] header = lines[0].split("\\|");
        if(header.length != EXPECTED_HEADER_FIELDS){
            throw new InvalidFastaFormatException("FASTA header for Patient should have exactly " + EXPECTED_HEADER_FIELDS + " fields separated by '|'");
        }
        return header;
    }
}
