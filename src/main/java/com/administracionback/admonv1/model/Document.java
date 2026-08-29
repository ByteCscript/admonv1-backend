package com.administracionback.admonv1.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, unique = true)
    private String s3Key;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;
}
