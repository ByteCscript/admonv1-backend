package com.administracionback.admonv1.dto;

import java.util.Map;

/*
 * Resumen previo al sorteo de cupos (C.U-08, paso 2).
 *
 * - approvedCount: solicitudes APPROVED, que son las que participan (RN-17).
 * - unvalidatedCount: solicitudes PENDING_VALIDATION; deben revisarse antes
 *   de ejecutar el sorteo (RN-37).
 * - canExecute / blockingError: si el sorteo se puede ejecutar y, si no, el
 *   código de error que devolvería la ejecución.
 * - directAssignment: los cupos alcanzan para todas las aprobadas, así que
 *   todas quedarán asignadas, sin lista de espera (RN-17).
 */
public record DrawPreviewDTO(
        Long callId,
        String callTitle,
        String callStatus,
        Integer totalSlots,
        long approvedCount,
        long unvalidatedCount,
        Map<String, Long> applicationsByStatus,
        boolean drawExecuted,
        boolean canExecute,
        String blockingError,
        boolean directAssignment
) {
}
