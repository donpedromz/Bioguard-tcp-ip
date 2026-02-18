package com.donpedromz.fasta.file;

import com.donpedromz.domain.diagnostic.DiagnosticRegistration;

import java.util.List;

/**
 * @version 1.0
 * @author juanp
 * Clase especializada en escanear archivos FASTA de diagnóstico,
 * asegurándose de que el formato del encabezado y la secuencia sean correctos,
 * y proporcionando mensajes de error claros en caso de que el formato no sea válido.
 */
public class DiagnosticScanner extends FileScanner<DiagnosticRegistration> {
    /**
     * {@inheritDoc}
     * @param content
     * @return
     */
    @Override
    protected List<DiagnosticRegistration> mapFileContent(String content) {
        List<String> lines = getLines(content);
        if (lines.size() != 2) {
            throw new FastaScanException("El archivo FASTA de muestra debe tener exactamente 2 líneas.");
        }
        String[] headerFields = parseHeader(lines.get(0), ">documento|fecha");
        String document = headerFields[0].trim();
        String date = headerFields[1].trim();
        if (document.isEmpty() || date.isEmpty()) {
            throw new FastaScanException("El encabezado FASTA debe contener documento y fecha.");
        }
        if (!date.matches(DATE_FORMAT_REGEX)) {
            throw new FastaScanException("El segundo campo del encabezado FASTA debe ser una fecha con formato YYYY-MM-DD.");
        }
        String sequence = lines.get(1);
        if (sequence.isEmpty()) {
            throw new FastaScanException("La secuencia FASTA no puede estar vacía.");
        }
        return List.of(new DiagnosticRegistration(document, date, sequence));
    }
}
