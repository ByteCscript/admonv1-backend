package com.administracionback.admonv1.dto;

import java.util.UUID;

public record DocumentPresignedResponseDTO(
        UUID documentId,
        String fileName,
        String uploadUrl
) {
}
