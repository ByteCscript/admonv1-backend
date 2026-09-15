package com.administracionback.admonv1.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "towers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tower {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
}
