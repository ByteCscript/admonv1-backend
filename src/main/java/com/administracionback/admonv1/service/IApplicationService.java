package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.*;
import org.springframework.http.ResponseEntity;

public interface IApplicationService {
    ResponseEntity<ApiResponse<ApplicationResponseDTO>> createApplication(
            ApplicationRequestDTO request
    );

    ResponseEntity<ApiResponse<ApplicationResponseDTO>> getApplication(
            Long applicationId
    );

    ResponseEntity<ApiResponse<PageResponseDTO<ApplicationResponseDTO>>> getApplications(
            ApplicationFilterDTO filters,
            int page,
            int size,
            String sortBy,
            String direction
    );
}
