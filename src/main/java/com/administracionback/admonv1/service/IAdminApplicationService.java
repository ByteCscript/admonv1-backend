package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.AdminApplicationDetailResponseDTO;
import com.administracionback.admonv1.dto.AdminApplicationResponseDTO;
import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.PageResponseDTO;
import org.springframework.http.ResponseEntity;

/*
 * Contrato de la revisión administrativa de postulaciones (CU 1.2.4, RN-11).
 *
 * Separado de IApplicationService para no mezclar el flujo del residente
 * con el del administrador (decisión D-6).
 */
public interface IAdminApplicationService {

    ResponseEntity<ApiResponse<PageResponseDTO<AdminApplicationResponseDTO>>>
    getAdminApplications(
            Long callId,
            String status,
            int page,
            int size
    );

    ResponseEntity<ApiResponse<AdminApplicationDetailResponseDTO>>
    getAdminApplicationDetail(Long applicationId);

    ResponseEntity<ApiResponse<AdminApplicationResponseDTO>> acceptApplication(
            Long applicationId,
            String adminEmail
    );

    ResponseEntity<ApiResponse<AdminApplicationResponseDTO>> rejectApplication(
            Long applicationId,
            String reason,
            String adminEmail
    );
}
