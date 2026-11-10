package com.administracionback.admonv1.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/*
 * Huella de integridad del sorteo de cupos (CU 1.1.2, D3).
 *
 * Almacena el SHA-256 de los bytes exactos del CSV exportado para una
 * convocatoria, permitiendo a residentes y administradores verificar que el
 * resultado publicado no fue alterado tras la ejecución del sorteo.
 *
 * Una fila por convocatoria (call_id); draw_id referencia el CallDraw que
 * originó el CSV.
 */
@Entity
@Table(name = "draw_integrity_hash")
@Getter @Setter
public class DrawIntegrityHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_id", nullable = false, unique = true)
    private Long callId;

    @Column(name = "draw_id", nullable = false)
    private Long drawId;

    @Column(name = "hash_value", nullable = false, length = 64)
    private String hashValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
