package com.donpedromz.dtos;

import com.donpedromz.model.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @author juanp
 * DTO que encapsula el resultado de un proceso de diagnóstico exitoso,
 * incluyendo el diagnóstico generado y los mensajes de las operaciones realizadas.
 */
public class DiagnoseResult {
    private final Diagnostic diagnostic;
    private final List<String> operationMessages;
    /**
     * Construye el resultado del diagnóstico.
     * @param diagnostic diagnóstico generado. No debe ser null.
     * @param operationMessages mensajes de las operaciones realizadas durante la persistencia.
     */
    public DiagnoseResult(Diagnostic diagnostic, List<String> operationMessages) {
        this.diagnostic = diagnostic;
        this.operationMessages = operationMessages == null ? new ArrayList<>() : new ArrayList<>(operationMessages);
    }
    public Diagnostic getDiagnostic() {
        return diagnostic;
    }
    public List<String> getOperationMessages() {
        return new ArrayList<>(operationMessages);
    }
}
