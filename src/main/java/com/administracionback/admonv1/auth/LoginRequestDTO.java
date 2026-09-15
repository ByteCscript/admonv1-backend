package com.administracionback.admonv1.auth;

public record LoginRequestDTO(
        String email,
        String password
) {
}
