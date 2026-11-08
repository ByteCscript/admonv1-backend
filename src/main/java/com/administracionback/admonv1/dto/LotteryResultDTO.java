package com.administracionback.admonv1.dto;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Resultado del sorteo de cupos (CU 1.1.2).
 *
 * Se devuelve tanto al ejecutar (POST /lottery) como al consultar (GET /lottery).
 * winners contiene solo las postulaciones seleccionadas (lotteryWon = true),
 * ordenadas por lotteryPosition.
 */
public record LotteryResultDTO(

        Long callId,
        String callTitle,

        LocalDateTime lotteryExecutedAt,
        String lotteryExecutedBy,

        int totalApproved,
        int totalWinners,

        List<LotteryWinnerDTO> winners

) {

    /*
     * Resumen de cada postulación ganadora del sorteo.
     */
    public record LotteryWinnerDTO(
            Long applicationId,
            String applicationNumber,
            Long residentId,
            String residentName,
            String residentEmail,
            String apartmentNumber,
            String towerName,
            int lotteryPosition
    ) {}
}
