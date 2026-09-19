package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.*;
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


    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDTO<ApplicationResponseDTO>>>
    getApplications(

            @RequestParam(required = false)
            Long residentId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(defaultValue = "createdAt")
            String sortBy,

            @RequestParam(defaultValue = "DESC")
            String direction
    ) {

        ApplicationFilterDTO filters =
                new ApplicationFilterDTO(residentId);

        return applicationService.getApplications(
                filters,
                page,
                size,
                sortBy,
                direction
        );
    }
}
