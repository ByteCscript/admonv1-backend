package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.CallDraw;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallDrawRepository extends JpaRepository<CallDraw, Long> {

    boolean existsByCallId(Long callId);

    Optional<CallDraw> findByCallId(Long callId);
}
