package com.administracionback.admonv1.dto;

/*
 * Una solicitud en el resultado del sorteo (C.U-08, RN-20).
 * waitlistPosition es null para las solicitudes SELECTED.
 */
public record DrawEntryDTO(
        int position,
        Long applicationId,
        String applicationNumber,
        String residentName,
        String apartmentNumber,
        String towerName,
        String status,
        Integer waitlistPosition
) {
}
