package com.administracionback.admonv1.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Detalle de postulación para la vista propia de Administración
 * (CU 1.2.4, decisión Q5): datos de la postulación, residente/apartamento,
 * convocatoria, estado, documentos y auditoría de la revisión.
 */
public record AdminApplicationDetailResponseDTO(
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
        LocalDate callStartDate,
        LocalDate callEndDate,
        Integer callAvailableSlots,
        String status,
        LocalDateTime createdAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        String drawResult,
        boolean callDrawn,
        List<DocumentSummary> documents
) {

    /*
     * Resumen de documento requerido por el panel admin (sin datos de S3).
     */
    public record DocumentSummary(
            UUID id,
            String documentType,
            String documentTypeLabel,
            String originalName,
            String status,
            Long size,
            LocalDateTime uploadedAt
    ) {
    }
}
