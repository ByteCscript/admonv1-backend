package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawResultRowDTO;
import com.administracionback.admonv1.dto.DrawResultsDTO;
import com.administracionback.admonv1.dto.IntegrityHashDTO;
import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import com.administracionback.admonv1.service.draw.DrawCsv;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * Consulta y exportación del resultado del sorteo (CU 1.1.2).
 *
 * El sorteo se ejecuta únicamente en DrawTransactions (C.U-08). Este servicio
 * solo lee lo que allí se registró; la huella de integridad se fija al
 * ejecutar el sorteo y exportar no la modifica (D3).
 */
@Service
@RequiredArgsConstructor
public class AdminDrawServiceImpl implements IAdminDrawService {

    private final CallRepository callRepository;
    private final ApplicationRepository applicationRepository;
    private final CallDrawRepository callDrawRepository;
    private final IDrawIntegrityHashService integrityHashService;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<DrawResultsDTO>> getDrawResults(
            Long callId,
            String viewerEmail,
            boolean viewerIsAdmin
    ) {

        ResponseEntity<ApiResponse<DrawResultsDTO>> invalid =
                validateCall(callId, new DrawResultsDTO(List.of(), null));

        if (invalid != null) {
            return invalid;
        }

        /*
         * R-11 (decisión del equipo, 2026-11-09): el administrador ve a todos
         * los participantes; el residente solo ve sus propias solicitudes.
         */
        List<DrawResultRowDTO> rows = applicationRepository
                .findByCallIdWithDrawResult(callId)
                .stream()
                .filter(application -> viewerIsAdmin
                        || (viewerEmail != null
                        && viewerEmail.equalsIgnoreCase(
                                application.getResident().getEmail())))
                .map(this::mapToRow)
                .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Resultado del sorteo consultado correctamente",
                        new DrawResultsDTO(
                                rows,
                                integrityHashService.getIntegrityHash(callId)
                        ),
                        null
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<IntegrityHashDTO>> getIntegrityHash(
            Long callId
    ) {

        ResponseEntity<ApiResponse<IntegrityHashDTO>> invalid =
                validateCall(callId, null);

        if (invalid != null) {
            return invalid;
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Huella de integridad consultada correctamente",
                        integrityHashService.getIntegrityHash(callId),
                        null
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<byte[]>> exportDrawCsv(Long callId) {

        ResponseEntity<ApiResponse<byte[]>> invalid =
                validateCall(callId, null);

        if (invalid != null) {
            return invalid;
        }

        if (!callDrawRepository.existsByCallId(callId)) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró un sorteo para esta convocatoria",
                    "DRAW_NOT_FOUND",
                    null);
        }

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "CSV del sorteo exportado correctamente",
                        DrawCsv.build(
                                applicationRepository.findByCallIdWithDrawResult(callId)
                        ),
                        null
                )
        );
    }

    private <T> ResponseEntity<ApiResponse<T>> validateCall(Long callId, T data) {

        if (callId == null || callId <= 0) {

            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID",
                    data);
        }

        if (!callRepository.existsById(callId)) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND",
                    data);
        }

        return null;
    }

    /*
     * D4: fila de resultado con torre, apartamento, residente, resultado,
     * estado y posición de sorteo.
     */
    private DrawResultRowDTO mapToRow(Application application) {

        return new DrawResultRowDTO(
                application.getId(),
                application.getApplicationNumber(),
                application.getApartment().getNumber(),
                application.getApartment().getTower().getName(),
                application.getResident().getName(),
                application.getDrawResult() == null
                        ? null
                        : application.getDrawResult().name(),
                application.getStatus().name(),
                application.getDrawPosition()
        );
    }

    private <T> ResponseEntity<ApiResponse<T>> error(
            HttpStatus status,
            String message,
            String code,
            T data
    ) {

        return ResponseEntity.status(status).body(
                new ApiResponse<>(message, data, code)
        );
    }
}
