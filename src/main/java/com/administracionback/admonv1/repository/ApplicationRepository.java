package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>,
        JpaSpecificationExecutor<Application> {

    Optional<Application> findByApartmentIdAndCallIdAndStatus(
            Long apartmentId,
            Long callId,
            ApplicationStatus status
    );
}