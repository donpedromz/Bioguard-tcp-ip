package com.donpedromz.format.factory;

import com.donpedromz.format.parser.FastaDiseaseParser;
import com.donpedromz.model.Disease;
/**
 * @version 1.0
 * @author juanp
 * Fábrica de parsers para entidades de tipo Disease.
 */
public class DiseaseParserFactory extends AbstractParserFactory<Disease>{
    public DiseaseParserFactory() {
        registerParser("application/fasta", new FastaDiseaseParser());
    }
}
