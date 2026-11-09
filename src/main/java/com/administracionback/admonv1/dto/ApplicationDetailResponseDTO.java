package com.administracionback.admonv1.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationDetailResponseDTO(
        Long id,
        String applicationNumber,
        Long residentId,
        String residentName,
        Long apartmentId,
        String apartmentNumber,
        Long callId,
        String callTitle,
        LocalDate callStartDate,
        LocalDate callEndDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String rejectionReason,
        String drawResult,
        List<DocumentResponseDTO> documents,
        Integer drawPosition,
        Integer waitlistPosition
) {
}
