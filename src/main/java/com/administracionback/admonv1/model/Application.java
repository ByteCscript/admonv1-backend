package com.administracionback.admonv1.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "applications"
)
@Getter
@Setter
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String applicationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private User resident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_id", nullable = false)
    private Call call;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /*
     * Posición asignada por el sorteo de cupos (C.U-08, RN-20), empezando
     * en 1. Es null mientras la convocatoria no se haya sorteado o si la
     * solicitud no participó (solo participan las APPROVED, RN-17).
     */
    @Column
    private Integer drawPosition;

    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL
    )
    private List<Document> documents = new ArrayList<>();
}
