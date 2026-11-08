package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /*
     * Usuarios del entorno de simulación masiva, con su apartamento ya cargado
     * (CU 1.1.2).
     */
    @Query("select u from User u left join fetch u.apartment "
            + "where u.simulated = true order by u.id")
    List<User> findSimulatedWithApartment();
}