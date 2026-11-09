package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>,
        JpaSpecificationExecutor<Application> {

    Optional<Application> findByApartmentIdAndCallIdAndStatus(
            Long apartmentId,
            Long callId,
            ApplicationStatus status
    );

    long countByCallId(Long callId);

    long countByCallIdAndStatus(
            Long callId,
            ApplicationStatus status
    );

    /* C.U-08 / CU 1.1.2: participantes del sorteo (RN-17). */
    List<Application> findByCallIdAndStatus(Long callId, ApplicationStatus status);

    /* C.U-08: resultado del sorteo, en el orden de las posiciones (RN-20). */
    List<Application> findByCallIdAndDrawPositionIsNotNullOrderByDrawPositionAsc(Long callId);

    /* CU 1.1.2: orden estable por id. */
    List<Application> findByCallIdAndStatusOrderByIdAsc(
            Long callId,
            ApplicationStatus status
    );

    /* CU 1.1.2: resultado ya sorteado, orden estable por id. */
    List<Application> findByCallIdAndDrawResultIsNotNullOrderByIdAsc(
            Long callId
    );

    /* CU 1.1.2 / D4: filas del resultado del sorteo con torre y residente,
       ordenadas por posición de sorteo. */
    @Query("""
            select a from Application a
            left join fetch a.apartment ap
            left join fetch ap.tower
            left join fetch a.resident u
            where a.call.id = :callId
            and a.drawResult is not null
            order by a.drawPosition asc nulls last, a.id asc
            """)
    List<Application> findByCallIdWithDrawResult(@Param("callId") Long callId);

    /* C.U-08: resumen previo al sorteo, solicitudes por estado. */
    @Query("""
            select a.status as status, count(a) as total
            from Application a
            where a.call.id = :callId
            group by a.status
            """)
    List<StatusCount> countByStatusForCall(@Param("callId") Long callId);

    interface StatusCount {
        ApplicationStatus getStatus();

        long getTotal();
    }

    /*
     * RN-01: una postulación vigente por apartamento y convocatoria.
     * Se consideran vigentes REGISTERED y APPROVED; CANCELLED/REJECTED liberan
     * el apartamento para volver a postularse.
     */
    Optional<Application> findFirstByApartmentIdAndCallIdAndStatusInOrderByIdAsc(
            Long apartmentId,
            Long callId,
            Collection<ApplicationStatus> statuses
    );

    /*
     * Lock pesimista usado al aceptar/rechazar (RN-13/RN-14).
     * Orden de bloqueo fijo en el servicio: Application -> Call.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") Long id);
}
