package com.administracionback.admonv1.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * drawExecuted (C.U-08): true si la convocatoria ya fue sorteada; en ese
 * caso no se puede modificar ni reabrir (RN-19).
 */
public record CallDTO(Long id,
                      String title,
                      String description,
                      LocalDate startDate,
                      LocalDate endDate,
                      Integer availableSlots,
                      String status,
                      boolean closedManually,
                      boolean drawExecuted,
                      boolean drawn,
                      LocalDateTime drawnAt) {
}
