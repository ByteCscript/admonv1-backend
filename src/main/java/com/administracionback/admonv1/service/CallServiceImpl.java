package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.CallDTO;
import com.administracionback.admonv1.dto.CallRequestDTO;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.CallDraw;
import com.administracionback.admonv1.model.CallStatus;
import com.administracionback.admonv1.model.CallStatusPolicy;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import com.administracionback.admonv1.service.SimulationEnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CallServiceImpl implements ICallService {

    /*
     * Misma zona horaria que ApplicationServiceImpl (RN-09).
     */
    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final CallRepository callRepository;
    private final ApplicationRepository applicationRepository;
    private final CallDrawRepository callDrawRepository;
    private final SimulationEnrollmentService simulationEnrollmentService;

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
                                "No se encontr\u00f3 la convocatoria",
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

        // CU 1.1.2: la simulaci\u00f3n masiva se preinscribe en cada convocatoria nueva.
        simulationEnrollmentService.enrollSimulated(saved);

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
                    "No se encontr\u00f3 la convocatoria",
                    "CALL_NOT_FOUND");
        }

        ResponseEntity<ApiResponse<CallDTO>> drawn = rejectIfDrawn(call);

        if (drawn != null) {
            return drawn;
        }

        if (call.isClosedManually()) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria est\u00e1 cerrada y no puede modificarse",
                    "CALL_CLOSED");
        }

        ResponseEntity<ApiResponse<CallDTO>> invalid =
                validateRequest(request, false);

        if (invalid != null) {
            return invalid;
        }

        /*
         * RN-22: una convocatoria sorteada solo admite cambios que no alteren
         * cupos ni el estado derivado (p. ej. corregir el t\u00edtulo).
         */
        boolean isDrawn = callDrawRepository.existsByCallId(call.getId());

        if (isDrawn
                && (!Objects.equals(request.totalSlots(),
                        call.getTotalSlots())
                || deriveStatus(call.isClosedManually(),
                        request.startDate(),
                        request.endDate()) != CallStatus.CLOSED)) {

            return error(HttpStatus.CONFLICT,
                    "La convocatoria ya fue sorteada: no se pueden cambiar cupos ni reabrir su per\u00edodo",
                    "CALL_ALREADY_DRAWN");
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
                    "No se encontr\u00f3 la convocatoria",
                    "CALL_NOT_FOUND");
        }

        if (call.isClosedManually()) {
            // Cierre manual ya aplicado: devolver el estado actual sin error
            // (cierre idempotente y graceful, RN-08).
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            "La convocatoria ya está cerrada",
                            mapToDTO(call),
                            null
                    )
            );
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
                    "No se encontr\u00f3 la convocatoria",
                    "CALL_NOT_FOUND");
        }

        ResponseEntity<ApiResponse<CallDTO>> drawn = rejectIfDrawn(call);

        if (drawn != null) {
            return drawn;
        }

        if (!call.isClosedManually()) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria no est\u00e1 cerrada",
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
                    "No se encontr\u00f3 la convocatoria",
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
                    "El identificador de la convocatoria es inv\u00e1lido",
                    "INVALID_CALL_ID");
        }
        return null;
    }

    /*
     * RN-09: \u00danica funci\u00f3n que decide el estado de una convocatoria.
     * Se usa al leer (mapToDTO) y al escribir (create/update/close/reopen),
     * re-grabando `status` como cach\u00e9 del estado derivado.
     */
    @Override
    public CallStatus deriveStatus(Call call) {
        return deriveStatus(
                call.isClosedManually(),
                call.getStartDate(),
                call.getEndDate()
        );
    }

    /*
     * Sobrecarga pura de RN-09 para validar escenarios sin entidad persistida
     * (p. ej. un update con fechas nuevas antes de aplicarlas).
     */
    public static CallStatus deriveStatus(
            boolean closedManually,
            LocalDate startDate,
            LocalDate endDate) {

        LocalDate today = LocalDate.now(COLOMBIA_ZONE);

        if (closedManually || today.isAfter(endDate)) {
            return CallStatus.CLOSED;
        }

        if (today.isBefore(startDate)) {
            return CallStatus.SCHEDULED;
        }
        return CallStatus.OPEN;
    }

    /*
     * RN-19 (C.U-08): una convocatoria sorteada no se puede modificar ni
     * reabrir, porque cambiar\u00eda las condiciones de un sorteo definitivo.
     */
    private ResponseEntity<ApiResponse<CallDTO>> rejectIfDrawn(Call call) {
        if (callDrawRepository.existsByCallId(call.getId())) {
            return error(HttpStatus.CONFLICT,
                    "La convocatoria ya fue sorteada y no puede modificarse ni reabrirse",
                    "DRAW_ALREADY_EXECUTED");
        }
        return null;
    }

    /*
     * RN-07: validaci\u00f3n manual de t\u00edtulo, fechas y cupos (sin jakarta.validation).
     * `checkPastStartDate` solo aplica al crear.
     */
    private ResponseEntity<ApiResponse<CallDTO>> validateRequest(
            CallRequestDTO request,
            boolean checkPastStartDate) {

        if (request == null || request.title() == null
                || request.title().isBlank()) {
            return error(HttpStatus.BAD_REQUEST,
                    "El t\u00edtulo de la convocatoria es obligatorio",
                    "CALL_TITLE_REQUIRED");
        }

        if (request.startDate() == null || request.endDate() == null
                || request.startDate().isAfter(request.endDate())) {
            return error(HttpStatus.BAD_REQUEST,
                    "Las fechas de la convocatoria no son v\u00e1lidas",
                    "CALL_INVALID_DATES");
        }

        if (checkPastStartDate
                && request.startDate().isBefore(LocalDate.now(COLOMBIA_ZONE))) {
            return error(HttpStatus.BAD_REQUEST,
                    "La fecha de inicio no puede ser anterior a hoy",
                    "CALL_INVALID_DATES");
        }

        // RN-07: cupos >= 1. Son los que se asignan en el sorteo y no cambian.
        if (request.totalSlots() == null || request.totalSlots() < 1) {
            return error(HttpStatus.BAD_REQUEST,
                    "Los cupos deben ser al menos 1",
                    "CALL_INVALID_SLOTS");
        }

        return null;
    }

    private void applyRequest(Call call, CallRequestDTO request) {
        call.setTitle(request.title().trim());
        call.setDescription(request.description());
        call.setStartDate(request.startDate());
        call.setEndDate(request.endDate());
        call.setTotalSlots(request.totalSlots());
    }

    /*
     * Al leer nunca se conf\u00eda en la columna `status`: se recalcula y, si
     * difiere de lo almacenado, se re-graba como cach\u00e9.
     */
    private CallDTO mapToDTO(Call call) {
        CallStatus derived = deriveStatus(call);

        if (call.getStatus() != derived) {
            call.setStatus(derived);
            callRepository.save(call);
        }

        // Deuda conocida: una consulta por convocatoria (N+1) para drawn/drawnAt.
        CallDraw draw = callDrawRepository.findByCallId(call.getId()).orElse(null);

        boolean drawExecuted = callDrawRepository.existsByCallId(call.getId());

        return new CallDTO(
                call.getId(),
                call.getTitle(),
                call.getDescription(),
                call.getStartDate(),
                call.getEndDate(),
                call.getTotalSlots(),
                derived.name(),
                call.isClosedManually(),
                drawExecuted,
                draw != null,
                draw == null ? null : draw.getExecutedAt()
        );
    }

    private ResponseEntity<ApiResponse<CallDTO>> error(
            HttpStatus status, String message, String code) {
        return ResponseEntity.status(status).body(
                new ApiResponse<>(message, null, code)
        );
    }
}
