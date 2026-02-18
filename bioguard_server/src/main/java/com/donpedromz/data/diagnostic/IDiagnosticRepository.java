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
	 * y una combinación de secuencia genética y fecha de muestra.
	 * Una muestra se considera duplicada solo si tanto la secuencia como la fecha coinciden.
	 * @param patientUuid El UUID del paciente para el cual se desea verificar la existencia del diagnóstico.
	 *                       No debe ser null.
	 * @param sampleSequence La secuencia genética de la muestra que se desea verificar.
	 *                                No debe ser null ni vacía.
	 * @param sampleDate La fecha de la muestra en formato YYYY-MM-DD. No debe ser null ni vacía.
	 * @param patientDocument El documento del paciente. No debe ser null ni vacío.
	 * @return {@code true} si existe un diagnóstico para el paciente con la misma secuencia y fecha,
	 * {@code false} en caso contrario.
	 */
	boolean existsByPatientAndSample(UUID patientUuid, String sampleSequence, String sampleDate, String patientDocument);
}
