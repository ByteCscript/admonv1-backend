package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.*;
import com.administracionback.admonv1.service.IApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final IApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> createApplication(
            @RequestBody ApplicationRequestDTO request) {

        return applicationService.createApplication(request);
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> getApplication(
            @PathVariable Long applicationId) {

        return applicationService.getApplication(applicationId);
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
