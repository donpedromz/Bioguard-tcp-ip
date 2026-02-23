package com.donpedromz.format.factory;

import com.donpedromz.format.parser.FastaPatientParser;
import com.donpedromz.model.Patient;
/**
 * @version 1.0
 * @author juanp
 * Fábrica de parsers para entidades de tipo Patient.
 */
public class PatientParserFactory extends AbstractParserFactory<Patient> {
    public PatientParserFactory() {
        registerParser("application/fasta", new FastaPatientParser());
    }
}
