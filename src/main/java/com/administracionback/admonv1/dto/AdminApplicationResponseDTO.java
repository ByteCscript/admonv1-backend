package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;

/*
 * Resumen de postulación para el panel de Administración (CU 1.2.4, RN-11).
 * No reemplaza a ApplicationResponseDTO: ese contrato pertenece al residente.
 */
public record AdminApplicationResponseDTO(
        Long id,
        String applicationNumber,
        Long residentId,
        String residentName,
        String residentEmail,
        Long apartmentId,
        String apartmentNumber,
        Long towerId,
        String towerName,
        Long callId,
        String callTitle,
        String status,
        LocalDateTime createdAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        String drawResult,
        long documentsCount
) {
}
