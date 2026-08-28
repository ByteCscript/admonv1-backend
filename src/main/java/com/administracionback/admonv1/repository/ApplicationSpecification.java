package com.administracionback.admonv1.repository;

import com.administracionback.admonv1.model.Application;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public class ApplicationSpecification {
    private ApplicationSpecification() {
    }

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
}
