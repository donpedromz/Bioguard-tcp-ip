package com.donpedromz.repositories.patient;

import com.donpedromz.model.Patient;

/**
 * @autor juanp
 * @version 1.0
 * Interfaz que define las operaciones de almacenamiento y recuperación de pacientes.
 */
public interface IPatientRepository {
    /**
     * Guarda un paciente en el almacenamiento.
     * @param entity El paciente a guardar. No debe ser null.
     */
    void save(Patient entity);

    /**
     * Recupera un paciente por su número de documento.
     * @param document El número de documento del paciente a recuperar. No debe ser null ni vacío.
     * @return El paciente correspondiente al número de documento proporcionado,
     * o null si no se encuentra ningún paciente con ese documento.
     */
    Patient getByDocument(String document);
}
