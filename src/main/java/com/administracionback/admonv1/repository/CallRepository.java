package com.administracionback.admonv1.repository;


import com.administracionback.admonv1.model.Call;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CallRepository extends JpaRepository<Call, Long> {

    /*
     * Lock pesimista de la convocatoria: serializa la revisión de sus
     * postulaciones con el sorteo. Siempre se toma antes del lock de la
     * postulación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Call c where c.id = :id")
    Optional<Call> findByIdForUpdate(@Param("id") Long id);
}
