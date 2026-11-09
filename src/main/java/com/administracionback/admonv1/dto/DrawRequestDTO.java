package com.administracionback.admonv1.dto;

/*
 * Semilla opcional del sorteo (CU 1.1.2).
 * Si no se envía, el servicio genera una semilla aleatoria reproducible.
 */
public record DrawRequestDTO(String seed) {
}
