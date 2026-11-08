package com.administracionback.admonv1.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/*
 * Registro de auditoría del sorteo de cupos de una convocatoria
 * (C.U-08, RN-19 y RN-21).
 *
 * - La restricción única sobre call_id garantiza un único sorteo por
 *   convocatoria, incluso si dos administradores lo ejecutan a la vez.
 * - @Immutable: Hibernate nunca emite UPDATE sobre esta tabla; el registro
 *   no se modifica después de creado.
 * - Con inputSnapshot, seed y algorithm cualquiera puede recalcular el orden
 *   y compararlo con resultSnapshot (ver DrawAlgorithm).
 */
@Entity
@Immutable
@Table(
        name = "call_draws",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_call_draw_call",
                columnNames = "call_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CallDraw {

    /*
     * Tamaño máximo de las listas serializadas (números de solicitud
     * separados por salto de línea). Con números "POST-<UUID>" (41
     * caracteres) alcanza para más de 4.000 participantes.
     */
    private static final int SNAPSHOT_LENGTH = 200_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_id", nullable = false, updatable = false)
    private Call call;

    /* Fecha y hora de ejecución en zona America/Bogota. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt;

    /* Correo del administrador que ejecutó el sorteo (sub del JWT). */
    @Column(nullable = false, updatable = false)
    private String executedBy;

    /* Cupos de la convocatoria al momento del sorteo. */
    @Column(nullable = false, updatable = false)
    private Integer availableSlots;

    @Column(nullable = false, updatable = false)
    private Integer participantsCount;

    /* Identificador del algoritmo (DrawAlgorithm.NAME). */
    @Column(nullable = false, updatable = false, length = 64)
    private String algorithm;

    /* Semilla aleatoria de 32 bytes, en hexadecimal. */
    @Column(nullable = false, updatable = false, length = 64)
    private String seed;

    /* SHA-256 (hex) de inputSnapshot. */
    @Column(nullable = false, updatable = false, length = 64)
    private String inputHash;

    /*
     * Lista de entrada: números de las solicitudes participantes ordenados
     * alfabéticamente, separados por salto de línea.
     */
    @Column(nullable = false, updatable = false, length = SNAPSHOT_LENGTH)
    private String inputSnapshot;

    /*
     * Resultado: números de solicitud en el orden del sorteo (posición 1
     * primero), separados por salto de línea.
     */
    @Column(nullable = false, updatable = false, length = SNAPSHOT_LENGTH)
    private String resultSnapshot;
}
