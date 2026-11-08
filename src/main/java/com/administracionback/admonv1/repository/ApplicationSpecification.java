package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Application;
import com.administracionback.admonv1.model.ApplicationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationSpecification {
    private ApplicationSpecification() {
    }

    /*
     * Filtro del residente (sin cambios: GET /api/applications).
     */
    public static Specification<Application> filter(
            Long residentId
    ) {

        return (root, query, criteriaBuilder) -> {

            Predicate predicate =
                    criteriaBuilder.conjunction();

            if (residentId != null) {

                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.equal(
                                root.get("resident").get("id"),
                                residentId
                        )
                );
            }

            return predicate;
        };
    }

    /*
     * Filtro del panel de Administración (CU 1.2.4, RN-11).
     * Convocatoria y estado son opcionales y se combinan con AND.
     */
    public static Specification<Application> adminFilter(
            Long callId,
            ApplicationStatus status
    ) {

        return (root, query, criteriaBuilder) -> {

            Predicate predicate =
                    criteriaBuilder.conjunction();

            if (callId != null) {

                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.equal(
                                root.get("call").get("id"),
                                callId
                        )
                );
            }

            if (status != null) {

                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.equal(
                                root.get("status"),
                                status
                        )
                );
            }

            return predicate;
        };
    }
}
