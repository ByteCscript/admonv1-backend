package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawResponseDTO;
import org.springframework.http.ResponseEntity;

/*
 * Sorteo de cupos de una convocatoria (CU 1.1.2).
 * Un solo sorteo por convocatoria; el resultado queda auditado en call_draws.
 */
public interface IAdminDrawService {

    ResponseEntity<ApiResponse<DrawResponseDTO>> executeDraw(
            Long callId,
            String seed,
            String adminEmail
    );

    ResponseEntity<ApiResponse<DrawResponseDTO>> getDraw(Long callId);
}
