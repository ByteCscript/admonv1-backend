package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Resultado del sorteo de cupos (CU 1.1.2).
 * Se devuelve al ejecutar (POST /api/admin/calls/{callId}/draw) y al
 * consultarlo (GET /api/admin/calls/{callId}/draw).
 */
public record DrawResponseDTO(
        Long drawId,
        Long callId,
        String callTitle,
        LocalDateTime executedAt,
        String executedBy,
        String seed,
        Integer availableSlotsBefore,
        Integer availableSlotsAfter,
        int participants,
        int selected,
        int notSelected,
        List<DrawResultItemDTO> results
) {

    /*
     * Resultado individual de una postulación participante.
     */
    public record DrawResultItemDTO(
            Long applicationId,
            String applicationNumber,
            Long apartmentId,
            String apartmentNumber,
            Long residentId,
            String residentName,
            String result,
            String status
    ) {
    }
}
