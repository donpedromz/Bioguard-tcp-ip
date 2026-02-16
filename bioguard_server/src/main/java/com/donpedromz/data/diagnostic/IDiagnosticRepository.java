package com.donpedromz.data.diagnostic;

import com.donpedromz.entities.Diagnostic;

import java.util.UUID;

/**
 * @version 1.0
 * @author juanp
 * Interfaz que representa el repositorio de diagnósticos.
 */
public interface IDiagnosticRepository {
	/**
	 * Guarda un diagnóstico en el repositorio. Esta operación puede implicar la escritura de datos en un archivo,
	 * base de datos u otra fuente de almacenamiento.
	 * @param entity La entidad de diagnóstico que se desea guardar.
	 *                  No debe ser null. La implementación debe validar los datos.
	 * @return Una cadena que representa el identificador único del diagnóstico guardado.
	 */
	String save(Diagnostic entity);

	/**
	 * Verifica si existe un diagnóstico en el repositorio para un paciente específico
	 * y un mensaje FASTA original dado.
	 * @param patientUuid El UUID del paciente para el cual se desea verificar la existencia del diagnóstico.
	 *                       No debe ser null.
	 * @param originalFastaMessage El mensaje FASTA original asociado al diagnóstico que se desea verificar.
	 *                                No debe ser null ni vacío.
	 * @return {@code true} si existe un diagnóstico para el paciente y mensaje FASTA original proporcionados,
	 * {@code false} en caso contrario.
	 */
	boolean existsByPatientAndSampleHash(UUID patientUuid, String originalFastaMessage);
}
