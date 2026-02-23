package com.donpedromz.service;

import com.donpedromz.model.Disease;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define las operaciones de negocio para el registro de enfermedades.
 */
public interface IDiseaseService {
    /**
     * Registra una nueva enfermedad en el sistema.
     * @param disease enfermedad a registrar. No debe ser null.
     * @return la enfermedad registrada con su UUID asignado.
     */
    Disease register(Disease disease);
}
