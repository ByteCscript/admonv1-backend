package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawResultsDTO;
import com.administracionback.admonv1.dto.IntegrityHashDTO;
import org.springframework.http.ResponseEntity;

/*
 * Consulta y exportación del resultado del sorteo (CU 1.1.2). La ejecución
 * del sorteo está en IDrawService (C.U-08).
 */
public interface IAdminDrawService {

    /* D5: exporta el CSV del sorteo (solo ROLE_ADMIN). */
    ResponseEntity<ApiResponse<byte[]>> exportDrawCsv(Long callId);

    /* D4: resultado del sorteo visible para residentes autenticados. */
    ResponseEntity<ApiResponse<DrawResultsDTO>> getDrawResults(Long callId);

    /* D4: huella de integridad visible para residentes autenticados. */
    ResponseEntity<ApiResponse<IntegrityHashDTO>> getIntegrityHash(Long callId);
}
