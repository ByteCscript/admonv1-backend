package com.administracionback.admonv1.dto;

public record CallDetailDTO(Long id,
                            String title,
                            String description,
                            Integer availableSlots,
                            String status) {
}
