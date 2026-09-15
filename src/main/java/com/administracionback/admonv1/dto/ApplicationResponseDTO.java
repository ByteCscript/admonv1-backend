package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;

public record ApplicationResponseDTO(
        Long id,
        String applicationNumber,
        Long residentId,
        String residentName,
        Long apartmentId,
        String apartmentNumber,
        Long callId,
        String callTitle,
        Long towerId,
        String towerName,
        String status,
        LocalDateTime createdAt
) {
}
