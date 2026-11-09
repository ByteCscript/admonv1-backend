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

    /*
     * Control de concurrencia optimista (RN-13).
     * Acompaña al lock pesimista sobre la fila al aceptar/rechazar.
     */
    @Version
    private Long version;

    /*
     * Auditoría mínima de la revisión administrativa (RN-15):
     * quién revisó, cuándo y, si aplica, el motivo del rechazo.
     */
    @Column(length = 120)
    private String reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String rejectionReason;

    @OneToMany(
            mappedBy = "application",
            cascade = CascadeType.ALL
    )
    private List<Document> documents = new ArrayList<>();

    /*
     * Resultado del sorteo (CU 1.1.2).
     * Solo se rellena cuando el administrador ejecuta el sorteo sobre la convocatoria.
     * lotteryWon=true  → postulación seleccionada (posición 1..N en lotteryPosition).
     * lotteryWon=false → postulación no seleccionada (lotteryPosition=null).
     * lotteryWon=null  → sorteo aún no ejecutado.
     */
    private Boolean lotteryWon;

    private Integer lotteryPosition;

    /*
     * Resultado del sorteo de cupos (CU 1.1.2).
     * SELECTED / NOT_SELECTED; null mientras la convocatoria no se ha sorteado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "draw_result", length = 20)
    private DrawResult drawResult;
}
