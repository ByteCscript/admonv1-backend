package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawResponseDTO;
import com.administracionback.admonv1.dto.DrawResultsDTO;
import com.administracionback.admonv1.dto.DrawResultRowDTO;
import com.administracionback.admonv1.dto.IntegrityHashDTO;
import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.CallDraw;
import com.administracionback.admonv1.model.CallStatus;
import com.administracionback.admonv1.model.DrawResult;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Sorteo de cupos (CU 1.1.2).
 *
 * Reglas:
 *  - Solo se sortea una convocatoria CLOSED (RN-09).
 *  - Un único sorteo por convocatoria (call_draws.call_id UNIQUE).
 *  - No se sortea con postulaciones PENDING_VALIDATION.
 *  - Participan únicamente las postulaciones REGISTERED.
 *  - Los seleccionados pasan a APPROVED y consumen cupos; el resto queda
 *    NOT_SELECTED sin cambiar de estado ni auditoría.
 *  - El orden es reproducible a partir de la semilla (SHA-256 -> Random).
 *
 * Integridad (D3): tras ejecutar el sorteo se calcula el SHA-256 de los
 * bytes exactos del CSV exportado y se persiste en draw_integrity_hash,
 * referenciando el CallDraw de la convocatoria.
 */
@Service
@RequiredArgsConstructor
public class AdminDrawServiceImpl implements IAdminDrawService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");
    private static final int MAX_SEED_LENGTH = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CallRepository callRepository;
    private final ApplicationRepository applicationRepository;
    private final CallDrawRepository callDrawRepository;
    private final ICallService callService;
    private final IDrawIntegrityHashService integrityHashService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<DrawResponseDTO>> executeDraw(
            Long callId,
            String seed,
            String adminEmail
    ) {

        if (callId == null || callId <= 0) {

            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID");
        }

        String normalizedSeed = seed == null ? null : seed.trim();

        if (normalizedSeed != null
                && normalizedSeed.length() > MAX_SEED_LENGTH) {

            return error(HttpStatus.BAD_REQUEST,
                    "La semilla no puede superar los "
                            + MAX_SEED_LENGTH + " caracteres",
                    "INVALID_SEED");
        }

        if (normalizedSeed == null || normalizedSeed.isBlank()) {
            normalizedSeed = generateSeed();
        }

        Call call = callRepository.findByIdForUpdate(callId).orElse(null);

        if (call == null) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND");
        }

        if (callService.deriveStatus(call) != CallStatus.CLOSED) {

            return error(HttpStatus.CONFLICT,
                    "La convocatoria debe estar cerrada para ejecutar el sorteo",
                    "CALL_NOT_CLOSED");
        }

        if (callDrawRepository.existsByCallId(callId)) {

            return error(HttpStatus.CONFLICT,
                    "El sorteo ya fue ejecutado para esta convocatoria",
                    "DRAW_ALREADY_EXECUTED");
        }

        if (applicationRepository.countByCallIdAndStatus(
                callId,
                ApplicationStatus.PENDING_VALIDATION) > 0) {

            return error(HttpStatus.CONFLICT,
                    "Existen postulaciones pendientes de validación documental",
                    "PENDING_REVIEW");
        }

        List<Application> participants = applicationRepository
                .findByCallIdAndStatusOrderByIdAsc(
                        callId,
                        ApplicationStatus.REGISTERED
                );

        if (participants.isEmpty()) {

            return error(HttpStatus.CONFLICT,
                    "La convocatoria no tiene postulaciones elegibles para sortear",
                    "NO_ELIGIBLE_APPLICATIONS");
        }

        int slotsBefore = call.getAvailableSlots() == null
                ? 0
                : call.getAvailableSlots();

        int selected = Math.min(slotsBefore, participants.size());

        LocalDateTime executedAt = LocalDateTime.now(COLOMBIA_ZONE);

        Set<Long> winnerIds = pickWinners(
                participants,
                selected,
                normalizedSeed
        );

        int position = 0;
        for (Application application : participants) {

            position++;
            application.setDrawPosition(position);

            if (winnerIds.contains(application.getId())) {

                application.setStatus(ApplicationStatus.APPROVED);
                application.setDrawResult(DrawResult.SELECTED);
                application.setReviewedBy(adminEmail);
                application.setReviewedAt(executedAt);

            } else {

                application.setDrawResult(DrawResult.NOT_SELECTED);
            }
        }

        applicationRepository.saveAll(participants);

        call.setAvailableSlots(slotsBefore - selected);
        call.setStatus(callService.deriveStatus(call));
        callRepository.save(call);

        CallDraw callDraw = new CallDraw();

        callDraw.setCall(call);
        callDraw.setExecutedAt(executedAt);
        callDraw.setExecutedBy(adminEmail);
        callDraw.setSeed(normalizedSeed);
        callDraw.setAvailableSlotsBefore(slotsBefore);
        callDraw.setAvailableSlotsAfter(slotsBefore - selected);
        callDraw.setParticipantsCount(participants.size());
        callDraw.setSelectedCount(selected);

        CallDraw savedDraw = callDrawRepository.save(callDraw);

        /*
         * D3: huella de integridad del CSV que se exportará. Se genera a
         * partir de los mismos participantes y orden (draw_position) que el
         * endpoint de exportación, por lo tanto ambos comparten el mismo hash.
         */
        byte[] csvBytes = buildCsvBytes(participants);
        integrityHashService.generateAndSaveHash(callId, csvBytes);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Sorteo ejecutado correctamente",
                        buildResponse(savedDraw, participants),
                        null
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<DrawResponseDTO>> getDraw(Long callId) {

        if (callId == null || callId <= 0) {

            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID");
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND");
        }

        CallDraw callDraw = callDrawRepository
                .findByCallId(callId)
                .orElse(null);

        if (callDraw == null) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró un sorteo para esta convocatoria",
                    "DRAW_NOT_FOUND");
        }

        List<Application> results = applicationRepository
                .findByCallIdAndDrawResultIsNotNullOrderByIdAsc(callId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Sorteo consultado correctamente",
                        buildResponse(callDraw, results),
                        null
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<DrawResultsDTO>> getDrawResults(
            Long callId
    ) {

        if (callId == null || callId <= 0) {

            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID",
                    new DrawResultsDTO(List.of(), null)
            );
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND",
                    new DrawResultsDTO(List.of(), null)
            );
        }

        List<Application> results = applicationRepository
                .findByCallIdWithDrawResult(callId);

        List<DrawResultRowDTO> rows = results.stream()
                .map(this::mapToRow)
                .collect(Collectors.toList());

        IntegrityHashDTO integrityHash = integrityHashService
                .getIntegrityHash(callId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Resultado del sorteo consultado correctamente",
                        new DrawResultsDTO(rows, integrityHash),
                        null
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<IntegrityHashDTO>> getIntegrityHash(
            Long callId
    ) {

        if (callId == null || callId <= 0) {

            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID",
                    null
            );
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND",
                    null
            );
        }

        IntegrityHashDTO integrityHash = integrityHashService
                .getIntegrityHash(callId);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "Huella de integridad consultada correctamente",
                        integrityHash,
                        null
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<byte[]>> exportDrawCsv(Long callId) {

        if (callId == null || callId <= 0) {

            return error(HttpStatus.BAD_REQUEST,
                    "El identificador de la convocatoria es inválido",
                    "INVALID_CALL_ID",
                    null
            );
        }

        Call call = callRepository.findById(callId).orElse(null);

        if (call == null) {

            return error(HttpStatus.NOT_FOUND,
                    "No se encontró la convocatoria",
                    "CALL_NOT_FOUND",
                    null
            );
        }

        List<Application> results = applicationRepository
                .findByCallIdWithDrawResult(callId);

        /*
         * CSV ordenado por draw_position (misma consulta que getDrawResults).
         * La huella se (re)genera sobre estos mismos bytes para garantizar
         * que el archivo descargado coincide con draw_integrity_hash (D3).
         */
        byte[] csvBytes = buildCsvBytes(results);
        integrityHashService.generateAndSaveHash(callId, csvBytes);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "CSV del sorteo exportado correctamente",
                        csvBytes,
                        null
                )
        );
    }

    /*
     * Copia ordenada por id + shuffle determinista: misma semilla, mismo
     * resultado. Los primeros `selected` son los ganadores.
     */
    private Set<Long> pickWinners(
            List<Application> participants,
            int selected,
            String seed
    ) {

        List<Application> ordered = new ArrayList<>(participants);

        ordered.sort(Comparator.comparing(Application::getId));

        Collections.shuffle(ordered, new Random(seedToLong(seed)));

        return ordered.subList(0, selected)
                .stream()
                .map(Application::getId)
                .collect(Collectors.toSet());
    }

    /*
     * SHA-256 del seed -> primeros 8 bytes -> long (semilla del Random).
     */
    private long seedToLong(String seed) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    seed.getBytes(StandardCharsets.UTF_8)
            );

            long value = 0L;

            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (hash[i] & 0xFFL);
            }

            return value;

        } catch (NoSuchAlgorithmException unavailable) {

            throw new IllegalStateException(
                    "El algoritmo SHA-256 no está disponible",
                    unavailable
            );
        }
    }

    private String generateSeed() {

        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);

        StringBuilder builder = new StringBuilder(bytes.length * 2);

        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }

        return builder.toString();
    }

    private DrawResponseDTO buildResponse(
            CallDraw callDraw,
            List<Application> results
    ) {

        List<DrawResponseDTO.DrawResultItemDTO> items = results
                .stream()
                .sorted(Comparator.comparing(Application::getId))
                .map(application ->
                        new DrawResponseDTO.DrawResultItemDTO(
                                application.getId(),
                                application.getApplicationNumber(),
                                application.getApartment().getId(),
                                application.getApartment().getNumber(),
                                application.getResident().getId(),
                                application.getResident().getName(),
                                application.getDrawResult() == null
                                        ? null
                                        : application.getDrawResult().name(),
                                application.getStatus().name()
                        ))
                .toList();

        long selected = results.stream()
                .filter(application ->
                        application.getDrawResult() == DrawResult.SELECTED)
                .count();

        int notSelected = results.size() - (int) selected;

        return new DrawResponseDTO(
                callDraw.getId(),
                callDraw.getCall().getId(),
                callDraw.getCall().getTitle(),
                callDraw.getExecutedAt(),
                callDraw.getExecutedBy(),
                callDraw.getSeed(),
                callDraw.getAvailableSlotsBefore(),
                callDraw.getAvailableSlotsAfter(),
                callDraw.getParticipantsCount(),
                (int) selected,
                notSelected,
                items
        );
    }

    /*
     * D4: mapea una postulación sorteada a su fila de resultado, incluyendo
     * torre, apartamento, residente, resultado, estado y posición de sorteo.
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

    /*
     * Genera los bytes del CSV con las columnas acordadas (D2), ordenados
     * por draw_position. Cada campo se escapa según RFC 4180.
     */
    private byte[] buildCsvBytes(List<Application> results) {

        StringBuilder builder = new StringBuilder();

        builder.append("application_id");
        builder.append(",");
        builder.append("application_number");
        builder.append(",");
        builder.append("apartment_number");
        builder.append(",");
        builder.append("resident_name");
        builder.append(",");
        builder.append("resident_id");
        builder.append(",");
        builder.append("result");
        builder.append(",");
        builder.append("status");
        builder.append(",");
        builder.append("draw_position");
        builder.append("\n");

        for (Application application : results) {

            builder.append(csvField(application.getId()));
            builder.append(",");
            builder.append(csvField(application.getApplicationNumber()));
            builder.append(",");
            builder.append(csvField(application.getApartment().getNumber()));
            builder.append(",");
            builder.append(csvField(application.getResident().getName()));
            builder.append(",");
            builder.append(csvField(application.getResident().getId()));
            builder.append(",");
            builder.append(csvField(
                    application.getDrawResult() == null
                            ? ""
                            : application.getDrawResult().name()));
            builder.append(",");
            builder.append(csvField(application.getStatus().name()));
            builder.append(",");
            builder.append(csvField(application.getDrawPosition()));
            builder.append("\n");
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /*
     * Escapa un campo de CSV según RFC 4180: rodea entre comillas dobles si
     * contiene coma, comilla o salto de línea, duplicando las comillas
     * internas.
     */
    private String csvField(Object value) {

        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);

        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
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

    private ResponseEntity<ApiResponse<DrawResponseDTO>> error(
            HttpStatus status,
            String message,
            String code
    ) {

        return ResponseEntity.status(status).body(
                new ApiResponse<>(message, null, code)
        );
    }
}
