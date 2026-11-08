package com.administracionback.admonv1.config;

import com.administracionback.admonv1.dto.ApiResponse;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
 * Traduce conflictos de concurrencia a un 409 con el contrato ApiResponse
 * estándar (CU 1.2.4, RN-13/RN-15).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleConcurrentUpdate(
            RuntimeException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        "La postulación fue modificada por otra operación. Intente nuevamente",
                        null,
                        "APPLICATION_CONCURRENT_UPDATE"
                )
        );
    }
}
