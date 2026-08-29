package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponseDTO(
        UUID id,
        String originalName,
        String contentType,
        Long size,
        String s3Key,
        LocalDateTime createdAt,
        LocalDateTime uploadedAt
) {
}
