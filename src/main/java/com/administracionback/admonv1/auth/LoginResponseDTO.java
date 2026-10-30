package com.administracionback.admonv1.auth;

import java.util.List;

public record LoginResponseDTO(
        String token,
        String type,
        long expiresIn,
        String email,
        String name,
        List<String> roles
) {
}
