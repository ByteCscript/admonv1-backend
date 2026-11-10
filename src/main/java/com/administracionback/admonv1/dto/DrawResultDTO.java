package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Resultado y auditoría del sorteo de cupos (C.U-08, RN-20 y RN-21).
 *
 * verified: el servidor recalcula el sorteo con inputApplicationNumbers,
 * seed y algorithm, y comprueba que coincida con el hash registrado, con el
 * orden registrado y con las posiciones guardadas en las solicitudes.
 *
 * directAssignment: los cupos alcanzaron para todas las aprobadas (RN-17).
 */
public record DrawResultDTO(
        Long callId,
        String callTitle,
        Integer totalSlots,
        int participantsCount,
        int selectedCount,
        int waitlistCount,
        LocalDateTime executedAt,
        String executedBy,
        String algorithm,
        String seed,
        String inputHash,
        List<String> inputApplicationNumbers,
        boolean verified,
        boolean directAssignment,
        List<DrawEntryDTO> entries
) {
}
