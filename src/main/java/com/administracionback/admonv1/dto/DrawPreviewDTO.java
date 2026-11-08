package com.administracionback.admonv1.dto;

import java.util.Map;

/*
 * Resumen previo al sorteo de cupos (C.U-08, paso 2 y FA-01).
 *
 * - approvedCount: solicitudes APPROVED, que son las que participan (RN-17).
 * - unvalidatedCount: solicitudes REGISTERED o PENDING_VALIDATION, que no
 *   participarán si el sorteo se ejecuta ahora (advertencia de FA-01).
 * - canExecute / blockingError: si el sorteo se puede ejecutar y, si no, el
 *   código de error que devolvería la ejecución.
 */
public record DrawPreviewDTO(
        Long callId,
        String callTitle,
        String callStatus,
        Integer availableSlots,
        long approvedCount,
        long unvalidatedCount,
        Map<String, Long> applicationsByStatus,
        boolean drawExecuted,
        boolean canExecute,
        String blockingError
) {
}
