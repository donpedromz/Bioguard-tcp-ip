package com.donpedromz.fasta.file;

import com.donpedromz.domain.disease.DiseaseRegistration;

import java.util.List;

/**
 * @version 1.0
 * @author juanp
 * Clase que extiende FastaFileScanner para mapear el contenido de un archivo FASTA a un objeto DiseaseRegistration,
 * asegurándose de que el formato del encabezado sea correcto y que la secuencia genética esté presente y no esté vacía.
 */
public class DiseaseScanner extends FastaFileScanner<DiseaseRegistration> {
    /**
     * {@inheritDoc}
     * @param content
     * @return
     */
    @Override
    protected List<DiseaseRegistration> mapFileContent(String content) {
        List<String> lines = getLines(content);
        if (lines.size() < 2) {
            throw new FastaScanException("El archivo FASTA debe contener encabezado y secuencia.");
        }

        String[] headerParts = parseHeader(lines.getFirst(), ">nombre|nivel");
        String name = headerParts[0].trim();
        String level = headerParts[1].trim();
        if (level.matches(DATE_FORMAT_REGEX)) {
            throw new FastaScanException("El archivo corresponde a un diagnóstico, no a una enfermedad.");
        }

        StringBuilder sequenceBuilder = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) {
            String sequenceLine = lines.get(i);
            for (int j = 0; j < sequenceLine.length(); j++) {
                char current = sequenceLine.charAt(j);
                if (!Character.isWhitespace(current)) {
                    sequenceBuilder.append(current);
                }
            }
        }

        String sequence = sequenceBuilder.toString();
        if (sequence.isEmpty()) {
            throw new FastaScanException("La secuencia FASTA no puede estar vacía.");
        }

        return List.of(new DiseaseRegistration(name, level, sequence));
    }
}
