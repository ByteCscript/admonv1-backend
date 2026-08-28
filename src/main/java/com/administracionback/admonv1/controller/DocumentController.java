package com.administracionback.admonv1.controller;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DocumentPresignedRequestDTO;
import com.administracionback.admonv1.dto.DocumentPresignedResponseDTO;
import com.administracionback.admonv1.dto.DocumentResponseDTO;
import com.administracionback.admonv1.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentService documentService;

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<DocumentPresignedResponseDTO>>
    generatePresignedUrl(
            @RequestBody DocumentPresignedRequestDTO request) {

        return documentService.generatePresignedUrl(request);
    }

    @PostMapping("/{documentId}/complete")
    public ResponseEntity<ApiResponse<DocumentResponseDTO>>
    completeUpload(
            @PathVariable UUID documentId) {

        return documentService.completeUpload(documentId);
    }
}
