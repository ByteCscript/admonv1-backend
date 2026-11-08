package com.administracionback.admonv1.dto;

/*
 * Motivo de rechazo (RN-15). Es opcional: si no llega, la postulación
 * se rechaza sin motivo y el residente ve el mensaje genérico.
 */
public record ApplicationRejectRequestDTO(
        String reason
) {
}
