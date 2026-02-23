package com.donpedromz.repositories.diagnostic;

import com.donpedromz.model.Diagnostic;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que representa el repositorio de informes de pacientes con alta infecciosidad.
 */
public interface IHighInfectivityPatientReportRepository {
    /**
     * Guarda un informe de paciente con alta infecciosidad en el repositorio.
     * @param diagnostic La entidad de diagnóstico que se desea guardar como
     *                   informe de alta infecciosidad. No debe ser null.
     * @return Una cadena que representa el identificador único del informe de alta infecciosidad guardado.
     */
    String save(Diagnostic diagnostic);
}
