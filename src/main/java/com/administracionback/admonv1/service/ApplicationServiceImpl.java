package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.*;
import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Document;
import com.administracionback.admonv1.model.DocumentStatus;
import com.administracionback.admonv1.model.DocumentType;
import com.administracionback.admonv1.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements IApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CallRepository callRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> createApplication(
            ApplicationRequestDTO request, String email) {

        if (request.callId() == null || email == null) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "La convocatoria y el residente son obligatorios",
                            null,
                            "INVALID_REQUEST"
                    )
            );
        }

        var call = callRepository.findById(request.callId());

        if (call.isEmpty()) {

            return ResponseEntity.notFound().build();
        }

        var resident = userRepository.findByEmail(email);

        if (resident.isEmpty()) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiResponse<>(
                            "No se encontró el residente",
                            null,
                            "RESIDENT_NOT_FOUND"
                    )
            );
        }

        var apartment = resident.get().getApartment();

        if (apartment == null) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new ApiResponse<>(
                            "El residente no tiene un apartamento asociado",
                            null,
                            "APARTMENT_NOT_FOUND"
                    )
            );
        }

        boolean exists = applicationRepository
                .existsByApartmentIdAndCallId(
                        apartment.getId(),
                        call.get().getId()
                );

        if (exists) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ApiResponse<>(
                            "El apartamento ya tiene una postulación para esta convocatoria",
                            null,
                            "APPLICATION_ALREADY_EXISTS"
                    )
            );
        }

        Application application = new Application();

        List<Document> documents = new ArrayList<>();

        if (request.documentIds() != null
                && !request.documentIds().isEmpty()) {

            documents = documentRepository.findAllById(
                    request.documentIds()
            );

            if (documents.size() != request.documentIds().size()) {

                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(
                                "Uno o más documentos no existen",
                                null,
                                "DOCUMENT_NOT_FOUND"
                        )
                );
            }
        }

        boolean anyNotUploaded = documents.stream()
                .anyMatch(document ->
                        document.getStatus() != DocumentStatus.UPLOADED
                );

        if (anyNotUploaded) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "Uno o más documentos no han finalizado su carga",
                            null,
                            "DOCUMENT_NOT_UPLOADED"
                    )
            );
        }

        Set<DocumentType> providedTypes = documents.stream()
                .map(Document::getDocumentType)
                .collect(Collectors.toSet());

        List<String> missingRequired = Arrays.stream(DocumentType.values())
                .filter(DocumentType::isRequired)
                .filter(type -> !providedTypes.contains(type))
                .map(DocumentType::getLabel)
                .toList();

        if (!missingRequired.isEmpty()) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "Faltan documentos obligatorios: "
                                    + String.join(", ", missingRequired),
                            null,
                            "REQUIRED_DOCUMENTS_MISSING"
                    )
            );
        }

        application.setApplicationNumber(
                "POST-" + UUID.randomUUID()
        );

        application.setCall(call.get());
        application.setApartment(apartment);
        application.setResident(resident.get());
        application.setStatus(ApplicationStatus.REGISTERED);
        application.setCreatedAt(LocalDateTime.now(COLOMBIA_ZONE));

        Application savedApplication =
                applicationRepository.save(application);

        documents.forEach(document ->
                document.setApplication(application)
        );

        documentRepository.saveAll(documents);


        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación registrada correctamente",
                        mapToDTO(savedApplication),
                        null
                )
        );
    }

    @Override
    public ResponseEntity<ApiResponse<ApplicationDetailResponseDTO>> getApplication(
            Long applicationId) {

        var application = applicationRepository.findById(applicationId);

        if (application.isEmpty()) {

            return ResponseEntity.notFound().build();
        }

        Application entity = application.get();


        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación consultada correctamente",
                        mapToDetailDTO(entity),
                        null
                )
        );
    }


    @Override
    public ResponseEntity<ApiResponse<PageResponseDTO<ApplicationResponseDTO>>> getApplications(ApplicationFilterDTO filters, int page, int size, String sortBy, String direction) {

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        PageRequest pageRequest =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(sortDirection, sortBy)
                );

        var specification =
                ApplicationSpecification.filter(
                        filters.residentId()
                );

        Page<Application> applicationPage =
                applicationRepository.findAll(
                        specification,
                        pageRequest
                );

        PageResponseDTO<ApplicationResponseDTO> response =
                new PageResponseDTO<>(
                        applicationPage.getContent()
                                .stream()
                                .map(this::mapToDTO)
                                .toList(),
                        applicationPage.getNumber(),
                        applicationPage.getSize(),
                        applicationPage.getTotalElements(),
                        applicationPage.getTotalPages(),
                        applicationPage.isFirst(),
                        applicationPage.isLast()
                );

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulaciones consultadas correctamente",
                        response,
                        null
                )
        );
    }

    private ApplicationResponseDTO mapToDTO(
            Application application
    ) {

        return new ApplicationResponseDTO(
                application.getId(),
                application.getApplicationNumber(),

                application.getResident().getId(),
                application.getResident().getName(),

                application.getApartment().getId(),
                application.getApartment().getNumber(),

                application.getCall().getId(),
                application.getCall().getTitle(),

                application.getApartment().getTower().getId(),
                application.getApartment().getTower().getName(),

                application.getStatus().name(),
                application.getCreatedAt()
        );
    }

    private ApplicationDetailResponseDTO mapToDetailDTO(
            Application application
    ) {

        return new ApplicationDetailResponseDTO(
                application.getId(),
                application.getApplicationNumber(),

                application.getResident().getId(),
                application.getResident().getName(),

                application.getApartment().getId(),
                application.getApartment().getNumber(),

                application.getCall().getId(),
                application.getCall().getTitle(),

                application.getStatus().name(),
                application.getCreatedAt(),

                application.getDocuments()
                        .stream()
                        .map(this::mapDocumentToDTO)
                        .toList()
        );
    }

    private DocumentResponseDTO mapDocumentToDTO(
            Document document
    ) {

        DocumentType type = document.getDocumentType();

        return new DocumentResponseDTO(
                document.getId(),
                document.getOriginalName(),
                document.getContentType(),
                document.getSize(),
                document.getS3Key(),
                document.getCreatedAt(),
                document.getUploadedAt(),
                type != null ? type.name() : null,
                type != null ? type.getLabel() : null
        );
    }

}