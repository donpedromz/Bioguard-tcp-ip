package com.donpedromz.data.disease;

import com.donpedromz.entities.Disease;

import java.util.List;

/**
 * @author juanp
 * @version 1.0
 * Interfaz que representa el repositorio de enfermedades para la aplicación BioGuard.
 * Define las operaciones relacionadas con el almacenamiento y recuperación de datos de enfermedades,
 * como guardar una enfermedad o recuperar todas las enfermedades almacenadas.
 */
public interface IDiseaseRepository {
	/**
	 * Guarda una enfermedad en el repositorio. Esta operación puede implicar la escritura de datos en un archivo,
	 * @param entity La entidad de enfermedad que se desea guardar.
	 *                  No debe ser null. La implementación debe manejar la validación de los datos
	 *                  y lanzar excepciones apropiadas en caso de errores durante el proceso de guardado.
	 */
	void save(Disease entity);

	/**
	 * Recupera todas las enfermedades almacenadas en el repositorio.
	 * Esta operación puede implicar la lectura de datos desde un archivo, base de datos u otra fuente de almacenamiento.
	 * @return Una lista de entidades de enfermedad que representan todas las enfermedades almacenadas en el repositorio.
	 */
	List<Disease> findAll();
}
