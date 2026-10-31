package com.administracionback.admonv1.dto;

import java.time.LocalDate;

/*
 * Body de POST /api/calls y PUT /api/calls/{id} (mismo DTO para ambos).
 * La validación es manual en CallServiceImpl: el repo no usa jakarta.validation.
 */
public record CallRequestDTO(String title,
                             String description,
                             LocalDate startDate,
                             LocalDate endDate,
                             Integer availableSlots) {
}
