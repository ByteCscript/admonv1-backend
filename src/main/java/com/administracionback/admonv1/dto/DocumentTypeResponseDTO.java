package com.administracionback.admonv1.dto;

public record DocumentTypeResponseDTO(
        String code,
        String label,
        boolean required,
        String purpose,
        long maxSizeBytes,
        String acceptedContentType
) {
}
