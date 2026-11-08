package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.DrawIntegrityHash;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/*
 * Repositorio de la huella de integridad del sorteo (CU 1.1.2, D3).
 */
public interface DrawIntegrityHashRepository extends JpaRepository<DrawIntegrityHash, Long> {
    Optional<DrawIntegrityHash> findByCallId(Long callId);
}
