package com.administracionback.admonv1.service;


import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.dto.CallRequestDTO;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.CallStatus;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallServiceImpl implements ICallService {

    /*
     * Misma zona horaria que ApplicationServiceImpl (RN-09).
     */
    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final CallRepository callRepository;
    private final ApplicationRepository applicationRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<List<CallDTO>>> getCalls() {

        List<CallDTO> calls = callRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();

        ApiResponse<List<CallDTO>> response =
                new ApiResponse<>(
                        "Convocatorias consultadas correctamente",
                        calls,
                        null
                );

        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<CallDTO>> getCall(Long callId) {

        ResponseEntity<ApiResponse<CallDTO>> invalid = validateCallId(callId);

        if (invalid != null) {
            return invalid;
        }

        return callRepository.findById(callId)
                .map(call -> ResponseEntity.ok(
                        new ApiResponse<>(
                                "Convocatoria consultada correctamente",
                                mapToDTO(call),
                                null
                        )))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(
                                "No se encontró la convocatoria",
                                null,
                                "CALL_NOT_FOUND")));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<CallDTO>> createCall(CallRequestDTO request) {

        ResponseEntity<ApiResponse<CallDTO>> invalid =
                validateRequest(request, true);

        if (invalid != null) {
            return invalid;
        }

        Call call = new Call();
        applyRequest(call, request);

        // closedManually nace en false (default de la columna).
        call.setStatus(deriveStatus(call));

        Call saved = callRepository.save(call);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Convocatoria creada correctamente",
                        mapToDTO(saved),
                        null
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<CallDTO>> updateCall(
            Long callId, CallRequestDTO request) {

        ResponseEntity<ApiResponse<CallDTO>> invalidId = validateCallId(callId);

        if (invalidId != null) {
            return invalidId;
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {
            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND");
        }

        if (call.isClosedManually()) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria está cerrada y no puede modificarse",
                    "CALL_CLOSED");
        }

        ResponseEntity<ApiResponse<CallDTO>> invalid =
                validateRequest(request, false);

        if (invalid != null) {
            return invalid;
        }

        applyRequest(call, request);
        call.setStatus(deriveStatus(call));

        Call saved = callRepository.save(call);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Convocatoria actualizada correctamente",
                        mapToDTO(saved),
                        null
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<CallDTO>> closeCall(Long callId) {

        ResponseEntity<ApiResponse<CallDTO>> invalid =
                validateCallId(callId);

        if (invalid != null) {
            return invalid;
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {
            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND");
        }

        if (call.isClosedManually()) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria ya está cerrada",
                    "CALL_ALREADY_CLOSED");
        }

        call.setClosedManually(true);
        call.setStatus(deriveStatus(call));

        Call saved = callRepository.save(call);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Convocatoria cerrada correctamente",
                        mapToDTO(saved),
                        null
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<CallDTO>> reopenCall(Long callId) {

        ResponseEntity<ApiResponse<CallDTO>> invalid =
                validateCallId(callId);

        if (invalid != null) {
            return invalid;
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {
            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND");
        }

        if (!call.isClosedManually()) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria no está cerrada",
                    "CALL_NOT_CLOSED");
        }

        call.setClosedManually(false);
        call.setStatus(deriveStatus(call));

        Call saved = callRepository.save(call);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Convocatoria reabierta correctamente",
                        mapToDTO(saved),
                        null
                )
        );
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<CallDTO>> deleteCall(Long callId) {

        ResponseEntity<ApiResponse<CallDTO>> invalid =
                validateCallId(callId);

        if (invalid != null) {
            return invalid;
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {
            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND");
        }

        if (applicationRepository.countByCallId(call.getId()) > 0) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria tiene postulaciones y no puede eliminarse",
                    "CALL_HAS_APPLICATIONS");
        }

        CallDTO dto = mapToDTO(call);

        callRepository.delete(call);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Convocatoria eliminada correctamente",
                        dto,
                        null
                )
        );
    }

    /*
     * Valida el identificador de la convocatoria (400 INVALID_CALL_ID si es nulo o <= 0).
     */
    private ResponseEntity<ApiResponse<CallDTO>> validateCallId(Long callId) {
        if (callId == null || callId <= 0) {
            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID");
        }
        return null;
    }

    /*
     * RN-09: única función que decide el estado de una convocatoria.
     * Se usa al leer (mapToDTO) y al escribir (create/update/close/reopen),
     * re-grabando `status` como caché del estado derivado.
     */
    private CallStatus deriveStatus(Call call) {
        LocalDate today = LocalDate.now(COLOMBIA_ZONE);

        if (call.isClosedManually() || today.isAfter(call.getEndDate())) {
            return CallStatus.CLOSED;
        }

        if (today.isBefore(call.getStartDate())) {
            return CallStatus.SCHEDULED;
        }

        return CallStatus.OPEN;
    }

    /*
     * RN-07: validación manual de título, fechas y cupos (sin jakarta.validation).
     * `checkPastStartDate` solo aplica al crear.
     */
    private ResponseEntity<ApiResponse<CallDTO>> validateRequest(
            CallRequestDTO request, boolean checkPastStartDate) {

        if (request == null || request.title() == null
                || request.title().isBlank()) {
            return error(HttpStatus.BAD_REQUEST,
                    "El título de la convocatoria es obligatorio",
                    "CALL_TITLE_REQUIRED");
        }

        if (request.startDate() == null || request.endDate() == null
                || request.startDate().isAfter(request.endDate())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Las fechas de la convocatoria no son válidas",
                    "CALL_INVALID_DATES");
        }

        if (checkPastStartDate
                && request.startDate().isBefore(LocalDate.now(COLOMBIA_ZONE))) {
            return error(HttpStatus.BAD_REQUEST,
                    "La fecha de inicio no puede ser anterior a hoy",
                    "CALL_INVALID_DATES");
        }

        if (request.availableSlots() == null || request.availableSlots() < 1) {
            return error(HttpStatus.BAD_REQUEST,
                    "Los cupos disponibles deben ser al menos 1",
                    "CALL_INVALID_SLOTS");
        }

        return null;
    }

    private void applyRequest(Call call, CallRequestDTO request) {
        call.setTitle(request.title().trim());
        call.setDescription(request.description());
        call.setStartDate(request.startDate());
        call.setEndDate(request.endDate());
        call.setAvailableSlots(request.availableSlots());
    }

    /*
     * Al leer nunca se confía en la columna `status`: se recalcula y, si
     * difiere de lo almacenado, se re-graba como caché.
     */
    private CallDTO mapToDTO(Call call) {
        CallStatus derived = deriveStatus(call);

        if (call.getStatus() != derived) {
            call.setStatus(derived);
            callRepository.save(call);
        }

        return new CallDTO(
                call.getId(),
                call.getTitle(),
                call.getDescription(),
                call.getStartDate(),
                call.getEndDate(),
                call.getAvailableSlots(),
                derived.name(),
                call.isClosedManually()
        );
    }

    private ResponseEntity<ApiResponse<CallDTO>> error(
            HttpStatus status, String message, String code) {
        return ResponseEntity.status(status).body(
                new ApiResponse<>(message, null, code)
        );
    }
}
