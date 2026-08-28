package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
}
