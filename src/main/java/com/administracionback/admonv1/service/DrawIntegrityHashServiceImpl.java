package com.administracionback.admonv1.service;

import com.administracionback.admonv1.dto.IntegrityHashDTO;
import com.administracionback.admonv1.model.CallDraw;
import com.administracionback.admonv1.model.DrawIntegrityHash;
import com.administracionback.admonv1.repository.CallDrawRepository;
import com.administracionback.admonv1.repository.DrawIntegrityHashRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/*
 * Implementación de la huella de integridad del sorteo (CU 1.1.2, D3).
 *
 * Calcula SHA-256 sobre los bytes exactos del CSV exportado y lo asocia a
 * la convocatoria, referenciando el CallDraw más reciente de esa
 * convocatoria.
 */
@Service
@RequiredArgsConstructor
public class DrawIntegrityHashServiceImpl implements IDrawIntegrityHashService {

    private static final ZoneId COLOMBIA_ZONE = ZoneId.of("America/Bogota");

    private final DrawIntegrityHashRepository integrityHashRepository;
    private final CallDrawRepository callDrawRepository;

    @Override
    @Transactional
    public IntegrityHashDTO generateAndSaveHash(Long callId, byte[] csvBytes) {

        String hashValue = sha256Hex(csvBytes);

        CallDraw latestDraw = callDrawRepository
                .findFirstByCallIdOrderByIdDesc(callId)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe un sorteo registrado para la convocatoria " + callId));

        DrawIntegrityHash hash = new DrawIntegrityHash();
        hash.setCallId(callId);
        hash.setDrawId(latestDraw.getId());
        hash.setHashValue(hashValue);
        hash.setCreatedAt(LocalDateTime.now(COLOMBIA_ZONE));

        DrawIntegrityHash saved = integrityHashRepository.save(hash);

        return new IntegrityHashDTO(saved.getHashValue(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrityHashDTO getIntegrityHash(Long callId) {

        return integrityHashRepository
                .findByCallId(callId)
                .map(hash -> new IntegrityHashDTO(hash.getHashValue(), hash.getCreatedAt()))
                .orElse(null);
    }

    /*
     * SHA-256 (hex) de los bytes recibidos.
     */
    private static String sha256Hex(byte[] bytes) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);

            StringBuilder builder = new StringBuilder(hash.length * 2);

            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }

            return builder.toString();

        } catch (NoSuchAlgorithmException unavailable) {

            throw new IllegalStateException(
                    "El algoritmo SHA-256 no está disponible",
                    unavailable
            );
        }
    }
}
