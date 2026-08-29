package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DocumentPresignedRequestDTO;
import com.administracionback.admonv1.dto.DocumentPresignedResponseDTO;
import com.administracionback.admonv1.dto.DocumentResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface IDocumentService {
    ResponseEntity<ApiResponse<DocumentPresignedResponseDTO>> generatePresignedUrl(
            DocumentPresignedRequestDTO request
    );

    ResponseEntity<ApiResponse<DocumentResponseDTO>> completeUpload(
            UUID documentId
    );
}
