package com.donpedromz.service;

import com.donpedromz.model.Patient;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define las operaciones de negocio para el registro de pacientes.
 */
public interface IPatientService {
    /**
     * Registra un nuevo paciente en el sistema.
     * @param patient paciente a registrar. No debe ser null.
     * @return el paciente registrado con su UUID asignado.
     */
    Patient register(Patient patient);
}
