package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawRequestDTO;
import com.administracionback.admonv1.dto.DrawResponseDTO;
import com.administracionback.admonv1.service.IAdminDrawService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Sorteo de cupos (CU 1.1.2).
 * RN-11/RN-22: todo /api/admin/** exige ROLE_ADMIN (SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/calls")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminDrawController {

    private final IAdminDrawService adminDrawService;

    @PostMapping("/{callId}/draw")
    public ResponseEntity<ApiResponse<DrawResponseDTO>> executeDraw(
            @PathVariable Long callId,
            @RequestBody(required = false) DrawRequestDTO request,
            Authentication authentication
    ) {

        String seed = request == null ? null : request.seed();

        return adminDrawService.executeDraw(
                callId,
                seed,
                authentication.getName()
        );
    }

    @GetMapping("/{callId}/draw")
    public ResponseEntity<ApiResponse<DrawResponseDTO>> getDraw(
            @PathVariable Long callId
    ) {

        return adminDrawService.getDraw(callId);
    }
}
