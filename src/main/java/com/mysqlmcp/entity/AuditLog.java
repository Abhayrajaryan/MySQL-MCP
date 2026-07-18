package com.mysqlmcp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One row per attempted database operation. Deliberately minimal — just
 * enough to answer who made the request, what they tried to do, and how
 * long it took (plus whether it succeeded, since that's essentially free
 * and central to "what happened").
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_connection_id")
    private DatabaseConnection databaseConnection;

    @Column(nullable = false)
    private String operation;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_time_ms", nullable = false)
    private Long executionTimeMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}