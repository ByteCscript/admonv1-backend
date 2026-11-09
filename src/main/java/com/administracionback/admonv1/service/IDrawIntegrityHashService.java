package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.IntegrityHashDTO;

/*
 * Servicio de la huella de integridad del sorteo de cupos (CU 1.1.2, D3).
 *
 * Genera y persiste el SHA-256 de los bytes exactos del CSV exportado, y
 * recupera la huella ya registrada para una convocatoria.
 */
public interface IDrawIntegrityHashService {

    IntegrityHashDTO generateAndSaveHash(Long callId, byte[] csvBytes);

    IntegrityHashDTO getIntegrityHash(Long callId);
}
