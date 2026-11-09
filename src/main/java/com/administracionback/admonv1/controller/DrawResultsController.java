package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawResultsDTO;
import com.administracionback.admonv1.dto.IntegrityHashDTO;
import com.administracionback.admonv1.service.IAdminDrawService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Resultado público del sorteo de cupos (CU 1.1.2, D4).
 *
 * Los residentes autenticados pueden consultar el listado de resultados
 * (nombre, apartamento, torre, resultado, estado, posición) y la huella de
 * integridad del CSV, sin acciones de administración.
 */
@RestController
@RequestMapping("/api/calls/{callId}/draw")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DrawResultsController {

    private final IAdminDrawService adminDrawService;

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<DrawResultsDTO>> getResults(
            @PathVariable Long callId
    ) {

        return adminDrawService.getDrawResults(callId);
    }

    @GetMapping("/hash")
    public ResponseEntity<ApiResponse<IntegrityHashDTO>> getHash(
            @PathVariable Long callId
    ) {

        return adminDrawService.getIntegrityHash(callId);
    }
}
