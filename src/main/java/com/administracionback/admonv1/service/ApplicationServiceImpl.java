package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.*;
import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.ApplicationSpecification;
import com.administracionback.admonv1.repository.CallRepository;
import com.administracionback.admonv1.repository.ResidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements IApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CallRepository callRepository;
    private final ResidentRepository residentRepository;

    @Override
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> createApplication(
            ApplicationRequestDTO request) {

        if (request.callId() == null || request.residentId() == null) {

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

        var resident = residentRepository.findById(request.residentId());

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

        application.setApplicationNumber(
                "POST-" + UUID.randomUUID()
        );

        application.setCall(call.get());
        application.setApartment(apartment);
        application.setResident(resident.get());
        application.setStatus(ApplicationStatus.REGISTERED);
        application.setCreatedAt(LocalDateTime.now());


        Application savedApplication =
                applicationRepository.save(application);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación registrada correctamente",
                        mapToDTO(savedApplication),
                        null
                )
        );
    }

    @Override
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> getApplication(
            Long applicationId) {

        var application = applicationRepository.findById(applicationId);

        if (application.isEmpty()) {

            return ResponseEntity.notFound().build();
        }

        Application entity = application.get();


        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Postulación consultada correctamente",
                        mapToDTO(entity),
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

                application.getStatus().name(),
                application.getCreatedAt()
        );
    }
}