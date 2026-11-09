package com.administracionback.admonv1.dto;

import java.util.List;

/*
 * Resultado completo del sorteo de cupos para una convocatoria (CU 1.1.2, D4).
 * Incluye el listado de filas (nombre, apartamento, torre, resultado,
 * estado, posición) y la huella de integridad del CSV exportado (D3).
 */
public record DrawResultsDTO(
        List<DrawResultRowDTO> results,
        IntegrityHashDTO integrityHash
) {
}
