package com.administracionback.admonv1.auth;

import java.util.List;

/*
 * userId / apartmentNumber / towerName permiten que el frontend muestre los
 * datos reales del residente autenticado (null para usuarios sin apartamento,
 * p. ej. la Administración).
 */
public record LoginResponseDTO(
        String token,
        String type,
        long expiresIn,
        String email,
        String name,
        List<String> roles,
        Long userId,
        String apartmentNumber,
        String towerName
) {
}
