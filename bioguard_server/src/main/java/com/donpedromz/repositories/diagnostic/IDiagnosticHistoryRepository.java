package com.donpedromz.repositories.diagnostic;
import com.donpedromz.model.Diagnostic;

/**
 * @version 1.0
 * @author juanp
 */
public interface IDiagnosticHistoryRepository {
    /**
     * Guarda un diagnóstico en el historial. Esta operación puede implicar la escritura de datos en un archivo,
     * base de datos u otra fuente de almacenamiento.
     * @param diagnostic La entidad de diagnóstico que se desea guardar. No debe ser null.
     *                   La implementación debe validar los datos.
     * @return Una cadena que representa el identificador único del diagnóstico guardado.
     */
    String save(Diagnostic diagnostic);
}
