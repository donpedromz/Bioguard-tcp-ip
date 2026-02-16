package com.donpedromz.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author juanp
 * @version 1.0
 * Clase que representa la informacion de la entidad Diagnostic
 */
public class Diagnostic {
	private UUID diagnosticUuid;
	private String sampleDate;
	private String sampleSequence;
	private Patient patient;
	private List<Disease> diseases;

	/**
	 * Constructor de la clase Diagnostic.
	 * @param diagnosticUuid El UUID único del diagnóstico. No debe ser null.
	 * @param sampleDate La fecha de la muestra en formato String. No debe ser null ni vacío.
	 * @param sampleSequence La secuencia genética extraída de la muestra FASTA. No debe ser null ni vacía.
	 * @param patient El paciente asociado al diagnóstico. No debe ser null.
	 * @param diseases La lista de enfermedades diagnosticadas. Si es null, se inicializa como una lista vacía.
	 */
	public Diagnostic(UUID diagnosticUuid, String sampleDate, String sampleSequence, Patient patient, List<Disease> diseases) {
		this.diagnosticUuid = diagnosticUuid;
		this.sampleDate = sampleDate;
		this.sampleSequence = sampleSequence;
		this.patient = patient;
		this.diseases = diseases == null ? new ArrayList<>() : new ArrayList<>(diseases);
	}

	public UUID getDiagnosticUuid() {
		return diagnosticUuid;
	}

	public void setDiagnosticUuid(UUID diagnosticUuid) {
		this.diagnosticUuid = diagnosticUuid;
	}

	public String getSampleDate() {
		return sampleDate;
	}

	public void setSampleDate(String sampleDate) {
		this.sampleDate = sampleDate;
	}

	public String getSampleSequence() {
		return sampleSequence;
	}

	public void setSampleSequence(String sampleSequence) {
		this.sampleSequence = sampleSequence;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public List<Disease> getDiseases() {
		return new ArrayList<>(diseases);
	}

	public void setDiseases(List<Disease> diseases) {
		this.diseases = diseases == null ? new ArrayList<>() : new ArrayList<>(diseases);
	}
}
