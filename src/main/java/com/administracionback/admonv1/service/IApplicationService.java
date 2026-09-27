package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.*;
import org.springframework.http.ResponseEntity;

public interface IApplicationService {
    ResponseEntity<ApiResponse<ApplicationResponseDTO>> createApplication(
            ApplicationRequestDTO request,
            String email
    );

    ResponseEntity<ApiResponse<ApplicationDetailResponseDTO>> getApplication(
            Long applicationId
    );

    ResponseEntity<ApiResponse<ApplicationEligibilityResponseDTO>> checkApplicationEligibility(
            Long callId,
            String email
    );

    ResponseEntity<ApiResponse<PageResponseDTO<ApplicationResponseDTO>>> getApplications(
            ApplicationFilterDTO filters,
            int page,
            int size,
            String sortBy,
            String direction
    );
}
