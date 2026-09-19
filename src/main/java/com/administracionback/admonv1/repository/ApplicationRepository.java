package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>,
        JpaSpecificationExecutor<Application> {

    boolean existsByApartmentIdAndCallId(
            Long apartmentId,
            Long callId
    );

    Optional<Application> findByApartmentIdAndCallId(
            Long apartmentId,
            Long callId
    );
}