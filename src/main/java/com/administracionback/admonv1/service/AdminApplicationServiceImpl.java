package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.AdminApplicationDetailResponseDTO;
import com.administracionback.admonv1.dto.AdminApplicationResponseDTO;
import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.PageResponseDTO;
import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.Document;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.ApplicationSpecification;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/*
 * Reglas de la revisión administrativa de postulaciones (CU 1.2.4).
 *
 * RN-11: solo ROLE_ADMIN (garantizado en SecurityConfig) lista y revisa.
 * RN-12: transiciones válidas PENDING_VALIDATION->APPROVED, PENDING_VALIDATION->REJECTED,
 *        APPROVED->REJECTED (revocación). Cualquier otra -> 409.
 * Aprobar habilita la postulación para el sorteo (C.U-08, RN-17): no asigna
 * ni consume cupos, por lo que puede haber más aprobadas que cupos. Los cupos
 * solo se asignan al ejecutar el sorteo.
 * RN-15: auditoría (reviewedBy/reviewedAt/rejectionReason) en la misma transacción.
 * Tras el sorteo no se admiten cambios de revisión (CALL_ALREADY_DRAWN).
 *
 * Concurrencia: lock pesimista en orden fijo Call -> Application, el mismo
 * del sorteo (que bloquea la convocatoria antes de leer a sus participantes).
 */
@Service
@RequiredArgsConstructor
public class AdminApplicationServiceImpl implements IAdminApplicationService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private static final int MAX_REJECTION_REASON_LENGTH = 500;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;

    private final ApplicationRepository applicationRepository;
    private final CallRepository callRepository;
    private final CallDrawRepository callDrawRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<PageResponseDTO<AdminApplicationResponseDTO>>>
    getAdminApplications(
            Long callId,
            String status,
            int page,
            int size
    ) {

        if (callId != null && callId <= 0) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "El identificador de la convocatoria es inválido",
                            null,
                            "INVALID_CALL_ID"
                    )
            );
        }

        ApplicationStatus parsedStatus;

        try {
            parsedStatus = parseStatus(status);
        } catch (IllegalArgumentException invalidStatus) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "El estado de la postulación es inválido",
                            null,
                            "INVALID_STATUS"
                    )
            );
        }

        int safePage = Math.max(page, 0);

        int safeSize = size <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Application> applicationPage = applicationRepository.findAll(
                ApplicationSpecification.adminFilter(callId, parsedStatus),
                pageRequest
        );

        PageResponseDTO<AdminApplicationResponseDTO> response =
                new PageResponseDTO<>(
                        applicationPage.getContent()
                                .stream()
                                .map(this::mapToAdminDTO)
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

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<AdminApplicationDetailResponseDTO>>
    getAdminApplicationDetail(Long applicationId) {

        if (applicationId == null || applicationId <= 0) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "El identificador de la postulación es inválido",
                            null,
                            "INVALID_APPLICATION_ID"
                    )
            );
        }

        Application application = applicationRepository
                .findById(applicationId)
                .orElse(null);

        if (application == null) {

            return applicationNotFound();
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación consultada correctamente",
                        mapToAdminDetailDTO(application),
                        null
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<AdminApplicationResponseDTO>>
    acceptApplication(Long applicationId, String adminEmail) {

        if (applicationId == null || applicationId <= 0 || adminEmail == null) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "La postulación y el administrador son obligatorios",
                            null,
                            "INVALID_REQUEST"
                    )
            );
        }

        Call call = lockCallOf(applicationId);

        if (call == null) {

            return applicationNotFound();
        }

        Application application = applicationRepository
                .findByIdForUpdate(applicationId)
                .orElse(null);

        if (application == null) {

            return applicationNotFound();
        }

        /*
         * RN-12: solo una postulación PENDING_VALIDATION puede aprobarse
         * (la operación no es idempotente).
         */
        if (application.getStatus() != ApplicationStatus.PENDING_VALIDATION) {

            return invalidState(
                    "La postulación no puede aprobarse en su estado actual"
            );
        }

        /*
         * RN-22: tras el sorteo ya no se aceptan postulaciones manualmente.
         */
        if (callDrawRepository.existsByCallId(call.getId())) {

            return callAlreadyDrawn();
        }

        validateReviewWindow(call);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedBy(adminEmail);
        application.setReviewedAt(LocalDateTime.now(COLOMBIA_ZONE));
        application.setRejectionReason(null);

        Application saved = applicationRepository.save(application);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación aprobada correctamente",
                        mapToAdminDTO(saved),
                        null
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<AdminApplicationResponseDTO>>
    rejectApplication(Long applicationId, String reason, String adminEmail) {

        if (applicationId == null || applicationId <= 0 || adminEmail == null) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "La postulación y el administrador son obligatorios",
                            null,
                            "INVALID_REQUEST"
                    )
            );
        }

        String normalizedReason = reason == null ? null : reason.trim();

        if (normalizedReason != null && normalizedReason.isEmpty()) {

            normalizedReason = null;
        }

        /*
         * RN-32: el motivo es obligatorio, porque el residente lo consulta
         * en su solicitud, y tiene máximo 500 caracteres.
         */
        if (normalizedReason == null) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "El motivo del rechazo es obligatorio",
                            null,
                            "REJECTION_REASON_REQUIRED"
                    )
            );
        }

        if (normalizedReason.length() > MAX_REJECTION_REASON_LENGTH) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(
                            "El motivo del rechazo no puede superar los "
                                    + MAX_REJECTION_REASON_LENGTH
                                    + " caracteres",
                            null,
                            "REJECTION_REASON_TOO_LONG"
                    )
            );
        }

        Call call = lockCallOf(applicationId);

        if (call == null) {

            return applicationNotFound();
        }

        Application application = applicationRepository
                .findByIdForUpdate(applicationId)
                .orElse(null);

        if (application == null) {

            return applicationNotFound();
        }

        ApplicationStatus currentStatus = application.getStatus();

        /*
         * RN-12: PENDING_VALIDATION->REJECTED (rechazo) o
         * APPROVED->REJECTED (revocación, decisión Q1).
         */
        if (currentStatus != ApplicationStatus.PENDING_VALIDATION
                && currentStatus != ApplicationStatus.APPROVED) {

            return invalidState(
                    "La postulación no puede rechazarse en su estado actual"
            );
        }

        /*
         * El resultado del sorteo es definitivo: ni el rechazo ni la
         * revocación pueden alterar a sus participantes.
         */
        if (callDrawRepository.existsByCallId(call.getId())) {

            return callAlreadyDrawn();
        }

        validateReviewWindow(call);

        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewedBy(adminEmail);
        application.setReviewedAt(LocalDateTime.now(COLOMBIA_ZONE));
        application.setRejectionReason(normalizedReason);

        Application saved = applicationRepository.save(application);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación rechazada correctamente",
                        mapToAdminDTO(saved),
                        null
                )
        );
    }

    /*
     * Punto de extensión para el CU 1.1.2 (sorteo): hoy no hay restricción
     * temporal para revisar postulaciones (decisión D-5).
     */
    private void validateReviewWindow(Call call) {
        // Sin restricciones por ahora.
    }

    /*
     * Primer lock del orden fijo Call -> Application (evita deadlocks con el
     * sorteo). Solo consulta el id de la convocatoria para no cargar la
     * postulación antes de bloquearla.
     */
    private Call lockCallOf(Long applicationId) {
        return applicationRepository
                .findCallIdById(applicationId)
                .flatMap(callRepository::findByIdForUpdate)
                .orElse(null);
    }

    private ApplicationStatus parseStatus(String status) {

        if (status == null || status.isBlank()) {

            return null;
        }

        return ApplicationStatus.valueOf(status.trim().toUpperCase());
    }

    private <T> ResponseEntity<ApiResponse<T>>
    applicationNotFound() {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(
                        "No se encontró la postulación",
                        null,
                        "APPLICATION_NOT_FOUND"
                )
        );
    }

    private <T> ResponseEntity<ApiResponse<T>>
    callAlreadyDrawn() {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        "La convocatoria ya fue sorteada; no admite cambios de revisión",
                        null,
                        "CALL_ALREADY_DRAWN"
                )
        );
    }

    private <T> ResponseEntity<ApiResponse<T>>
    invalidState(String message) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        message,
                        null,
                        "APPLICATION_INVALID_STATE"
                )
        );
    }

    private AdminApplicationResponseDTO mapToAdminDTO(
            Application application
    ) {

        List<Document> documents = application.getDocuments();

        return new AdminApplicationResponseDTO(
                application.getId(),
                application.getApplicationNumber(),

                application.getResident().getId(),
                application.getResident().getName(),
                application.getResident().getEmail(),

                application.getApartment().getId(),
                application.getApartment().getNumber(),

                application.getApartment().getTower().getId(),
                application.getApartment().getTower().getName(),

                application.getCall().getId(),
                application.getCall().getTitle(),

                application.getStatus().name(),
                application.getCreatedAt(),

                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getRejectionReason(),

                application.getDrawResult() == null
                        ? null
                        : application.getDrawResult().name(),

                documents == null ? 0 : documents.size()
        );
    }

    private AdminApplicationDetailResponseDTO mapToAdminDetailDTO(
            Application application
    ) {

        List<Document> documents = application.getDocuments();

        return new AdminApplicationDetailResponseDTO(
                application.getId(),
                application.getApplicationNumber(),

                application.getResident().getId(),
                application.getResident().getName(),
                application.getResident().getEmail(),

                application.getApartment().getId(),
                application.getApartment().getNumber(),

                application.getApartment().getTower().getId(),
                application.getApartment().getTower().getName(),

                application.getCall().getId(),
                application.getCall().getTitle(),
                application.getCall().getStartDate(),
                application.getCall().getEndDate(),
                application.getCall().getTotalSlots(),

                application.getStatus().name(),
                application.getCreatedAt(),

                application.getReviewedBy(),
                application.getReviewedAt(),
                application.getRejectionReason(),

                application.getDrawResult() == null
                        ? null
                        : application.getDrawResult().name(),

                callDrawRepository.existsByCallId(
                        application.getCall().getId()
                ),

                documents == null
                        ? List.of()
                        : documents.stream()
                                .map(this::mapToDocumentSummary)
                                .toList()
        );
    }

    private AdminApplicationDetailResponseDTO.DocumentSummary
    mapToDocumentSummary(Document document) {

        return new AdminApplicationDetailResponseDTO.DocumentSummary(
                document.getId(),
                document.getDocumentType() != null
                        ? document.getDocumentType().name()
                        : null,
                document.getDocumentType() != null
                        ? document.getDocumentType().getLabel()
                        : null,
                document.getOriginalName(),
                document.getStatus() != null
                        ? document.getStatus().name()
                        : null,
                document.getSize(),
                document.getUploadedAt()
        );
    }
}
