package com.donpedromz.format.factory;

import com.donpedromz.dtos.DiagnoseMessageDto;
import com.donpedromz.format.parser.FastaDiagnosticParser;
/**
 * @version 1.0
 * @author juanp
 * Fábrica de parsers para entidades de tipo DiagnoseMessageDto.
 */
public class DiagnosticParserFactory extends AbstractParserFactory<DiagnoseMessageDto> {
    public DiagnosticParserFactory() {
        registerParser("application/fasta", new FastaDiagnosticParser());
    }
}
