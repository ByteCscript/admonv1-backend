package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.AdminApplicationDetailResponseDTO;
import com.administracionback.admonv1.dto.AdminApplicationResponseDTO;
import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DocumentReadUrlResponseDTO;
import com.administracionback.admonv1.dto.PageResponseDTO;
import com.administracionback.admonv1.service.IAdminApplicationService;
import com.administracionback.admonv1.service.IDocumentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/*
 * Panel de postulaciones de Administración (CU 1.2.4, decisión D-6).
 *
 * Endpoint propio: el listado del residente (GET /api/applications) no cambia.
 * RN-11: todo /api/admin/** exige ROLE_ADMIN (SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminApplicationController {

    private final IAdminApplicationService adminApplicationService;

    private final IDocumentService documentService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDTO<AdminApplicationResponseDTO>>>
    getAdminApplications(

            @RequestParam(required = false)
            Long callId,

            @RequestParam(required = false)
            String status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "50")
            int size
    ) {

        return adminApplicationService.getAdminApplications(
                callId,
                status,
                page,
                size
        );
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AdminApplicationDetailResponseDTO>>
    getAdminApplicationDetail(
            @PathVariable Long applicationId) {

        return adminApplicationService.getAdminApplicationDetail(
                applicationId
        );
    }

    /*
     * URL de lectura prefirmada de un documento de la postulación (RN-23).
     * Queda bajo /api/admin/** → solo ROLE_ADMIN (SecurityConfig).
     */
    @GetMapping("/{applicationId}/documents/{documentId}/url")
    public ResponseEntity<ApiResponse<DocumentReadUrlResponseDTO>>
    generateDocumentReadUrl(
            @PathVariable Long applicationId,
            @PathVariable UUID documentId) {

        return documentService.generateReadUrl(
                applicationId,
                documentId
        );
    }
}
