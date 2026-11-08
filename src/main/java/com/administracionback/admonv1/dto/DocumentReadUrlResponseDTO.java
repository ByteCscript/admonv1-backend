package com.administracionback.admonv1.dto;

import java.util.UUID;

public record DocumentReadUrlResponseDTO(
        UUID documentId,
        String url,
        long expiresInMinutes
) {
}
