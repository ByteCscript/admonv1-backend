package com.administracionback.admonv1.service.draw;

import com.administracionback.admonv1.dto.DrawEntryDTO;
import com.administracionback.admonv1.dto.DrawPreviewDTO;
import com.administracionback.admonv1.dto.DrawResultDTO;
import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import com.administracionback.admonv1.model.Call;
import com.administracionback.admonv1.model.CallDraw;
import com.administracionback.admonv1.model.CallStatus;
import com.administracionback.admonv1.model.CallStatusPolicy;
import com.administracionback.admonv1.repository.ApplicationRepository;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.CallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * Operaciones transaccionales del sorteo de cupos (C.U-08).
 *
 * Los errores de negocio se lanzan como DrawException para que la
 * transacción se revierta completa (FE-06); DrawServiceImpl los traduce a
 * respuestas HTTP.
 */
@Component
@RequiredArgsConstructor
public class DrawTransactions {

    /* Estados que aún no se han validado: no participan (FA-01). */
    private static final Set<ApplicationStatus> UNVALIDATED =
            EnumSet.of(ApplicationStatus.REGISTERED, ApplicationStatus.PENDING_VALIDATION);

    private final SecureRandom secureRandom = new SecureRandom();

    private final CallRepository callRepository;
    private final ApplicationRepository applicationRepository;
    private final CallDrawRepository callDrawRepository;

    /* Paso 2 y FA-01: resumen previo al sorteo. */
    @Transactional(readOnly = true)
    public DrawPreviewDTO preview(Long callId) {

        Call call = findCall(callId);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        long approved = 0;
        long unvalidated = 0;

        for (ApplicationRepository.StatusCount count
                : applicationRepository.countByStatusForCall(callId)) {

            byStatus.put(count.getStatus().name(), count.getTotal());

            if (count.getStatus() == ApplicationStatus.APPROVED) {
                approved = count.getTotal();
            } else if (UNVALIDATED.contains(count.getStatus())) {
                unvalidated += count.getTotal();
            }
        }

        DrawError blocking = blockingError(call, approved);

        return new DrawPreviewDTO(
                call.getId(),
                call.getTitle(),
                CallStatusPolicy.derive(call).name(),
                call.getAvailableSlots(),
                approved,
                unvalidated,
                byStatus,
                callDrawRepository.existsByCallId(callId),
                blocking == null,
                blocking == null ? null : blocking.name()
        );
    }

    /* Pasos 6 a 11: ejecuta el sorteo y registra su auditoría. */
    @Transactional
    public DrawResultDTO execute(Long callId, String executedBy) {

        Call call = findCall(callId);

        List<Application> approved = applicationRepository
                .findByCallIdAndStatus(callId, ApplicationStatus.APPROVED);

        DrawError blocking = blockingError(call, approved.size());

        if (blocking == DrawError.INSUFFICIENT_APPROVED_APPLICATIONS) {
            throw new DrawException(blocking, String.format(
                    "Hay %d solicitudes aprobadas y la convocatoria tiene %d cupos; "
                            + "las solicitudes aprobadas deben superar el número de cupos",
                    approved.size(), call.getAvailableSlots()));
        }

        if (blocking != null) {
            throw new DrawException(blocking);
        }

        // Paso 8: lista de entrada canónica y su huella.
        Map<String, Application> byNumber = approved.stream()
                .collect(Collectors.toMap(Application::getApplicationNumber, Function.identity()));

        List<String> input = DrawAlgorithm.canonicalInput(List.copyOf(byNumber.keySet()));
        String inputSnapshot = DrawAlgorithm.serialize(input);

        // Paso 9: semilla aleatoria segura y orden reproducible.
        byte[] seed = new byte[DrawAlgorithm.SEED_BYTES];
        secureRandom.nextBytes(seed);

        List<String> order = DrawAlgorithm.shuffle(input, seed);

        // Paso 11: el registro va primero; si otro administrador ya registró
        // un sorteo para la convocatoria, la restricción única falla aquí y
        // no se modifica ninguna solicitud (FE-03).
        CallDraw draw = new CallDraw();
        draw.setCall(call);
        draw.setExecutedAt(LocalDateTime.now(CallStatusPolicy.COLOMBIA_ZONE));
        draw.setExecutedBy(executedBy);
        draw.setAvailableSlots(call.getAvailableSlots());
        draw.setParticipantsCount(order.size());
        draw.setAlgorithm(DrawAlgorithm.NAME);
        draw.setSeed(DrawAlgorithm.toHex(seed));
        draw.setInputHash(DrawAlgorithm.sha256Hex(inputSnapshot));
        draw.setInputSnapshot(inputSnapshot);
        draw.setResultSnapshot(DrawAlgorithm.serialize(order));

        callDrawRepository.saveAndFlush(draw);

        // Paso 10: posiciones y estados (RN-20).
        int slots = call.getAvailableSlots();
        List<Application> ordered = order.stream().map(byNumber::get).toList();

        for (int i = 0; i < ordered.size(); i++) {
            Application application = ordered.get(i);
            application.setDrawPosition(i + 1);
            application.setStatus(i < slots
                    ? ApplicationStatus.SELECTED
                    : ApplicationStatus.NOT_SELECTED);
        }

        // RN-19: el cierre queda definitivo; además impide nuevas solicitudes
        // (RN-29), porque la convocatoria pasa a estar cerrada manualmente.
        call.setClosedManually(true);
        call.setStatus(CallStatus.CLOSED);

        return toResult(call, draw, ordered);
    }

    /* Paso 13 y FA-03: resultado y auditoría de un sorteo registrado. */
    @Transactional(readOnly = true)
    public DrawResultDTO result(Long callId) {

        Call call = findCall(callId);

        CallDraw draw = callDrawRepository.findByCallId(callId)
                .orElseThrow(() -> new DrawException(DrawError.DRAW_NOT_FOUND));

        List<Application> ordered = applicationRepository
                .findByCallIdAndDrawPositionIsNotNullOrderByDrawPositionAsc(callId);

        return toResult(call, draw, ordered);
    }

    private Call findCall(Long callId) {
        return callRepository.findById(callId)
                .orElseThrow(() -> new DrawException(DrawError.CALL_NOT_FOUND));
    }

    /*
     * Mismo orden de validación que los flujos de excepción: cerrada (RN-18),
     * sin sorteo previo (RN-19) y más aprobadas que cupos (RN-17).
     */
    private DrawError blockingError(Call call, long approvedCount) {

        if (CallStatusPolicy.derive(call) != CallStatus.CLOSED) {
            return DrawError.CALL_NOT_CLOSED;
        }

        if (callDrawRepository.existsByCallId(call.getId())) {
            return DrawError.DRAW_ALREADY_EXECUTED;
        }

        if (approvedCount <= call.getAvailableSlots()) {
            return DrawError.INSUFFICIENT_APPROVED_APPLICATIONS;
        }

        return null;
    }

    private DrawResultDTO toResult(Call call, CallDraw draw, List<Application> ordered) {

        int slots = draw.getAvailableSlots();

        List<DrawEntryDTO> entries = ordered.stream()
                .map(application -> toEntry(application, slots))
                .toList();

        int selected = (int) entries.stream()
                .filter(entry -> entry.waitlistPosition() == null)
                .count();

        return new DrawResultDTO(
                call.getId(),
                call.getTitle(),
                slots,
                draw.getParticipantsCount(),
                selected,
                entries.size() - selected,
                draw.getExecutedAt(),
                draw.getExecutedBy(),
                draw.getAlgorithm(),
                draw.getSeed(),
                draw.getInputHash(),
                DrawAlgorithm.deserialize(draw.getInputSnapshot()),
                verify(draw, ordered),
                entries
        );
    }

    private DrawEntryDTO toEntry(Application application, int slots) {

        int position = application.getDrawPosition();

        return new DrawEntryDTO(
                position,
                application.getId(),
                application.getApplicationNumber(),
                application.getResident().getName(),
                application.getApartment().getNumber(),
                application.getApartment().getTower().getName(),
                application.getStatus().name(),
                position > slots ? position - slots : null
        );
    }

    /*
     * RN-21: recalcula el sorteo y lo compara con lo registrado. Detecta
     * cambios en la lista de entrada, en el orden guardado o en las
     * posiciones de las solicitudes.
     */
    private boolean verify(CallDraw draw, List<Application> ordered) {

        if (!DrawAlgorithm.NAME.equals(draw.getAlgorithm())) {
            return false;
        }

        if (!DrawAlgorithm.sha256Hex(draw.getInputSnapshot()).equals(draw.getInputHash())) {
            return false;
        }

        List<String> recomputed = DrawAlgorithm.shuffle(
                DrawAlgorithm.deserialize(draw.getInputSnapshot()),
                DrawAlgorithm.fromHex(draw.getSeed())
        );

        if (!recomputed.equals(DrawAlgorithm.deserialize(draw.getResultSnapshot()))) {
            return false;
        }

        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getDrawPosition() != i + 1) {
                return false;
            }
        }

        List<String> stored = ordered.stream()
                .map(Application::getApplicationNumber)
                .toList();

        return recomputed.equals(stored);
    }
}
