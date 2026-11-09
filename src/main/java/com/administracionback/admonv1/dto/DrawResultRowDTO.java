package com.administracionback.admonv1.dto;

/*
 * Fila del resultado del sorteo de cupos (CU 1.1.2, D4).
 * Datos de una postulación ya sorteada: nombre del residente, unidad
 * (torre + apartamento), resultado del sorteo y estado de la postulación.
 */
public record DrawResultRowDTO(
        Long applicationId,
        String applicationNumber,
        String apartmentNumber,
        String apartmentTower,
        String residentName,
        String result,
        String status,
        Integer drawPosition
) {
}
