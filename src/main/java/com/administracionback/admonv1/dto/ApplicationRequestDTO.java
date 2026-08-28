package com.administracionback.admonv1.dto;

import java.util.List;
import java.util.UUID;

public record ApplicationRequestDTO(
        Long callId,
        Long residentId,
        List<UUID> documentIds
)  {
}
