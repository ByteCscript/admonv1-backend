package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;

public record ApplicationEligibilityResponseDTO(
        boolean canApply,
        ApplicationSummaryDTO existingApplication
) {


    public record ApplicationSummaryDTO(
            Long id,
            String applicationNumber,
            String status,
            LocalDateTime createdAt
    ) {
    }
}