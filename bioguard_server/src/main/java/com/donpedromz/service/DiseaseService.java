package com.donpedromz.service;

import com.donpedromz.repositories.disease.IDiseaseRepository;
import com.donpedromz.model.Disease;

/**
 * @version 1.0
 * @author juanp
 * Servicio de negocio que implementa la lógica de registro de enfermedades.
 */
public class DiseaseService implements IDiseaseService {
    private final IDiseaseRepository diseaseRepository;

    /**
     * Construye el servicio de registro de enfermedades.
     * @param diseaseRepository repositorio de enfermedades. No debe ser null.
     */
    public DiseaseService(IDiseaseRepository diseaseRepository) {
        if (diseaseRepository == null) {
            throw new IllegalArgumentException("diseaseRepository no puede ser null");
        }
        this.diseaseRepository = diseaseRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Disease register(Disease disease) {
        diseaseRepository.save(disease);
        return disease;
    }
}
