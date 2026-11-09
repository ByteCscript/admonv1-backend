package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawResponseDTO;
import com.administracionback.admonv1.dto.DrawResultsDTO;
import com.administracionback.admonv1.dto.IntegrityHashDTO;
import org.springframework.http.ResponseEntity;

/*
 * Sorteo de cupos de una convocatoria (CU 1.1.2).
 * Un solo sorteo por convocatoria; el resultado queda auditado en call_draws
 * y su huella de integridad en draw_integrity_hash (D3).
 */
public interface IAdminDrawService {

    ResponseEntity<ApiResponse<DrawResponseDTO>> executeDraw(
            Long callId,
            String seed,
            String adminEmail
    );

    ResponseEntity<ApiResponse<DrawResponseDTO>> getDraw(Long callId);

    /* D5: exporta el CSV del sorteo (solo ROLE_ADMIN). */
    ResponseEntity<ApiResponse<byte[]>> exportDrawCsv(Long callId);

    /* D4: resultado del sorteo visible para residentes autenticados. */
    ResponseEntity<ApiResponse<DrawResultsDTO>> getDrawResults(Long callId);

    /* D4: huella de integridad visible para residentes autenticados. */
    ResponseEntity<ApiResponse<IntegrityHashDTO>> getIntegrityHash(Long callId);
}
