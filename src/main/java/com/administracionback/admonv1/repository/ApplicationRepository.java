package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /* C.U-08: participantes del sorteo (RN-17). */
    List<Application> findByCallIdAndStatus(Long callId, ApplicationStatus status);

    /* C.U-08: resultado del sorteo, en el orden de las posiciones (RN-20). */
    List<Application> findByCallIdAndDrawPositionIsNotNullOrderByDrawPositionAsc(Long callId);

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
}