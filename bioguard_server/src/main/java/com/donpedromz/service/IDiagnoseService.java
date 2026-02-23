package com.donpedromz.service;

import com.donpedromz.dtos.DiagnoseMessageDto;
import com.donpedromz.dtos.DiagnoseResult;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que define las operaciones de negocio para el proceso de diagnóstico.
 * Aplica las reglas de negocio necesarias para generar un diagnóstico a partir
 * de una muestra genética de un paciente.
 */
public interface IDiagnoseService {
    /**
     * Genera un diagnóstico a partir de la información contenida en el DTO de diagnóstico.
     * Busca al paciente, verifica duplicados, compara la secuencia genética contra
     * las enfermedades registradas y persiste el diagnóstico resultante.
     * @param diagnoseMessage DTO con documento del paciente, fecha de muestra y secuencia genética.
     * @return resultado del diagnóstico con el diagnóstico generado y los mensajes de operación.
     */
    DiagnoseResult diagnose(DiagnoseMessageDto diagnoseMessage);
}
