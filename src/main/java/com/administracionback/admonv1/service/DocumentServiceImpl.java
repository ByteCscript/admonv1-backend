package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DocumentPresignedRequestDTO;
import com.administracionback.admonv1.dto.DocumentPresignedResponseDTO;
import com.administracionback.admonv1.dto.DocumentResponseDTO;
import com.administracionback.admonv1.model.Document;
import com.administracionback.admonv1.model.DocumentStatus;
import com.administracionback.admonv1.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements IDocumentService {


    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final DocumentRepository documentRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presign-duration-minutes}")
    private long presignDurationMinutes;

    @Override
    public ResponseEntity<ApiResponse<DocumentPresignedResponseDTO>> generatePresignedUrl(DocumentPresignedRequestDTO request) {
        {

            UUID documentId = UUID.randomUUID();

            String key = "documents/"
                    + documentId
                    + "/"
                    + request.fileName();


            Document document = new Document();

            document.setId(documentId);
            document.setOriginalName(request.fileName());
            document.setContentType(request.contentType());
            document.setSize(request.size());
            document.setS3Key(key);
            document.setBucket(bucketName);
            document.setCreatedAt(LocalDateTime.now());
            document.setStatus(DocumentStatus.PENDING);

            documentRepository.save(document);

            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType(request.contentType())
                            .contentLength(request.size())
                            .build();

            PutObjectPresignRequest presignRequest =
                    PutObjectPresignRequest.builder()
                            .signatureDuration(
                                    Duration.ofMinutes(
                                            presignDurationMinutes))
                            .putObjectRequest(putObjectRequest)
                            .build();

            PresignedPutObjectRequest presigned =
                    s3Presigner.presignPutObject(presignRequest);

            DocumentPresignedResponseDTO response =
                    new DocumentPresignedResponseDTO(
                            documentId,
                            request.fileName(),
                            presigned.url().toString()
                    );

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            "URL generada correctamente",
                            response,
                            null
                    )
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<DocumentResponseDTO>> completeUpload(UUID documentId) {

        var documentOptional =
                documentRepository.findById(documentId);

        if (documentOptional.isEmpty()) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(
                            "No se encontró el documento",
                            null,
                            "DOCUMENT_NOT_FOUND"
                    )
            );
        }

        Document document = documentOptional.get();

        try {

            var headObjectResponse =
                    s3Client.headObject(
                            HeadObjectRequest.builder()
                                    .bucket(document.getBucket())
                                    .key(document.getS3Key())
                                    .build()
                    );

            document.setSize(headObjectResponse.contentLength());
            document.setContentType(headObjectResponse.contentType());
            document.setUploadedAt(LocalDateTime.now());
            document.setStatus(DocumentStatus.UPLOADED);

            documentRepository.save(document);

            DocumentResponseDTO response =
                    new DocumentResponseDTO(
                            document.getId(),
                            document.getOriginalName(),
                            document.getContentType(),
                            document.getSize(),
                            document.getS3Key(),
                            document.getCreatedAt(),
                            document.getUploadedAt()
                    );

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            "Documento cargado correctamente",
                            response,
                            null
                    )
            );

        } catch (NoSuchKeyException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(
                            "El archivo no existe en S3",
                            null,
                            "FILE_NOT_FOUND_IN_S3"
                    )
            );
        }
    }

}