package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.*;
import com.administracionback.admonv1.service.IAdminApplicationService;
import com.administracionback.admonv1.service.IApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final IApplicationService applicationService;
    private final IAdminApplicationService adminApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> createApplication(
            @RequestBody ApplicationRequestDTO request, Authentication authentication) {

        String email = authentication.getName();
        return applicationService.createApplication(request, email);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationDetailResponseDTO>> getApplication(
            @PathVariable Long applicationId) {

        return applicationService.getApplication(applicationId);
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<ApplicationEligibilityResponseDTO>> checkApplicationEligibility(
            @RequestParam Long callId,
            Authentication authentication) {

        String email = authentication.getName();

        return applicationService.checkApplicationEligibility(
                callId,
                email
        );
    }

    @PatchMapping("/{applicationId}/cancel")
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> cancelApplication(
            @PathVariable Long applicationId,
            Authentication authentication) {

        String email = authentication.getName();

        return applicationService.cancelApplication(
                applicationId,
                email
        );
    }

    /*
     * CU 1.2.4 (RN-11): solo ROLE_ADMIN llega aquí (SecurityConfig).
     */
    @PatchMapping("/{applicationId}/accept")
    public ResponseEntity<ApiResponse<AdminApplicationResponseDTO>> acceptApplication(
            @PathVariable Long applicationId,
            Authentication authentication) {

        return adminApplicationService.acceptApplication(
                applicationId,
                authentication.getName()
        );
    }

    @PatchMapping("/{applicationId}/reject")
    public ResponseEntity<ApiResponse<AdminApplicationResponseDTO>> rejectApplication(
            @PathVariable Long applicationId,
            @RequestBody(required = false) ApplicationRejectRequestDTO request,
            Authentication authentication) {

        String reason = request == null ? null : request.reason();

        return adminApplicationService.rejectApplication(
                applicationId,
                reason,
                authentication.getName()
        );
    }

    /*
     * "Mis postulaciones": siempre se limita al usuario autenticado (token).
     * El panel de Administración usa /api/admin/applications.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDTO<ApplicationResponseDTO>>>
    getApplications(

            Authentication authentication,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            String direction
    ) {

        return applicationService.getApplications(
                authentication.getName(),
                page,
                size,
                sortBy,
                direction
        );
    }
}
