package com.administracionback.admonv1.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Table(name = "calls")
@Getter
@Setter
public class Call {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer availableSlots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallStatus status;

    /*
     * Cierre administrativo de la convocatoria (RN-08).
     * El estado visible se deriva con deriveStatus(CallService);
     * esta columna solo registra el cierre manual.
     */
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean closedManually;
}
