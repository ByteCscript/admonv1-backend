package com.administracionback.admonv1.dto;

import java.time.LocalDate;

public record CallDTO(Long id,
                      String title,
                      String description,
                      LocalDate startDate,
                      LocalDate endDate,
                      Integer availableSlots,
                      String status,
                      boolean closedManually) {
}
