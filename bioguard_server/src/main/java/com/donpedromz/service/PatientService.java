package com.donpedromz.service;

import com.donpedromz.repositories.patient.IPatientRepository;
import com.donpedromz.model.Patient;

/**
 * @version 1.0
 * @author juanp
 * Servicio de negocio que implementa la lógica de registro de pacientes.
 */
public class PatientService implements IPatientService {
    private final IPatientRepository patientRepository;
    /**
     * Construye el servicio de registro de pacientes.
     * @param patientRepository repositorio de pacientes. No debe ser null.
     */
    public PatientService(IPatientRepository patientRepository) {
        if (patientRepository == null) {
            throw new IllegalArgumentException("patientRepository no puede ser null");
        }
        this.patientRepository = patientRepository;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Patient register(Patient patient) {
        patientRepository.save(patient);
        return patient;
    }
}
