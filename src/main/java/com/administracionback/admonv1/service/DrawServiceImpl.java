package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.ApiResponse;
import com.administracionback.admonv1.dto.DrawPreviewDTO;
import com.administracionback.admonv1.dto.DrawResultDTO;
import com.administracionback.admonv1.service.draw.DrawError;
import com.administracionback.admonv1.service.draw.DrawException;
import com.administracionback.admonv1.service.draw.DrawTransactions;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/*
 * C.U-08 · Ejecutar sorteo de cupos.
 *
 * Este servicio NO es transaccional a propósito: delega en DrawTransactions
 * y traduce sus errores a ApiResponse. Así puede capturar el fallo de la
 * restricción única uk_call_draw_call (dos administradores ejecutando el
 * sorteo a la vez, FE-03) después de que la transacción perdedora se
 * revirtió por completo.
 */
@Service
@RequiredArgsConstructor
public class DrawServiceImpl implements IDrawService {

    private final DrawTransactions drawTransactions;

    @Override
    public ResponseEntity<ApiResponse<DrawPreviewDTO>> getPreview(Long callId) {
        return run(callId, HttpStatus.OK,
                "Resumen del sorteo consultado correctamente",
                () -> drawTransactions.preview(callId));
    }

    @Override
    public ResponseEntity<ApiResponse<DrawResultDTO>> executeDraw(Long callId, String adminEmail) {
        try {
            return run(callId, HttpStatus.CREATED,
                    "Sorteo ejecutado correctamente",
                    () -> drawTransactions.execute(callId, adminEmail));

        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            // FE-03 bajo concurrencia: otra ejecución registró (o está
            // registrando) el sorteo de esta convocatoria.
            return error(DrawError.DRAW_ALREADY_EXECUTED, DrawError.DRAW_ALREADY_EXECUTED.message());
        }
    }

    @Override
    public ResponseEntity<ApiResponse<DrawResultDTO>> getResult(Long callId) {
        return run(callId, HttpStatus.OK,
                "Resultado del sorteo consultado correctamente",
                () -> drawTransactions.result(callId));
    }

    private <T> ResponseEntity<ApiResponse<T>> run(
            Long callId, HttpStatus okStatus, String okMessage, Supplier<T> action) {

        if (callId == null || callId <= 0) {
            return error(DrawError.INVALID_CALL_ID, DrawError.INVALID_CALL_ID.message());
        }

        try {
            return ResponseEntity.status(okStatus)
                    .body(new ApiResponse<>(okMessage, action.get(), null));

        } catch (DrawException e) {
            return error(e.error(), e.getMessage());
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> error(DrawError error, String message) {
        return ResponseEntity.status(error.status())
                .body(new ApiResponse<>(message, null, error.name()));
    }
}
