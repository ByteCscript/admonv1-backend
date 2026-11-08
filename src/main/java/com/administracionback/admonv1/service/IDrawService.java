package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawPreviewDTO;
import com.administracionback.admonv1.dto.DrawResultDTO;
import org.springframework.http.ResponseEntity;

/*
 * C.U-08 · Ejecutar sorteo de cupos.
 */
public interface IDrawService {

    ResponseEntity<ApiResponse<DrawPreviewDTO>> getPreview(Long callId);

    ResponseEntity<ApiResponse<DrawResultDTO>> executeDraw(Long callId, String adminEmail);

    ResponseEntity<ApiResponse<DrawResultDTO>> getResult(Long callId);
}
