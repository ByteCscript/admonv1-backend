package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawPreviewDTO;
import com.administracionback.admonv1.dto.DrawResultDTO;
import com.administracionback.admonv1.service.IDrawService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * C.U-08 · Ejecutar sorteo de cupos. Todos los endpoints exigen ROLE_ADMIN
 * (SecurityConfig, FE-05).
 */
@RestController
@RequestMapping("/api/calls/{callId}/draw")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DrawController {

    private final IDrawService drawService;

    @Operation(summary = "Resumen previo al sorteo: cupos, solicitudes por estado y si se puede ejecutar")
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<DrawPreviewDTO>> getPreview(
            @PathVariable Long callId) {

        return drawService.getPreview(callId);
    }

    @Operation(summary = "Ejecuta el sorteo de cupos de una convocatoria cerrada (definitivo, no se repite)")
    @PostMapping
    public ResponseEntity<ApiResponse<DrawResultDTO>> executeDraw(
            @PathVariable Long callId,
            Authentication authentication) {

        return drawService.executeDraw(callId, authentication.getName());
    }

    @Operation(summary = "Resultado y auditoría del sorteo, con verificación de reproducibilidad")
    @GetMapping
    public ResponseEntity<ApiResponse<DrawResultDTO>> getResult(
            @PathVariable Long callId) {

        return drawService.getResult(callId);
    }
}
