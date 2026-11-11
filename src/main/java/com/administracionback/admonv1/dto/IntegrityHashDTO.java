package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;

/*
 * DTO de la huella de integridad del sorteo (CU 1.1.2, D3).
 * Expuesta a residentes y administradores en /api/calls/{callId}/draw/hash
 * y dentro de /api/calls/{callId}/draw/results.
 */
public record IntegrityHashDTO(
        String hashValue,
        LocalDateTime createdAt
) {
}
