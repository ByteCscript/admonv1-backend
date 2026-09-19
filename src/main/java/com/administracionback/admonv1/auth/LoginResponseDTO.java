package com.administracionback.admonv1.auth;

public record LoginResponseDTO(
        String token,
        String type,
        long expiresIn
) {
}
