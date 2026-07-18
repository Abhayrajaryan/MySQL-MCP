package com.mysqlmcp.repository;

import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.enums.AuditSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Single flexible search used by the audit UI/API. Every filter is optional —
     * pass null to skip it. Kept as one query (rather than a Specification) to
     * match this codebase's preference for simple, explicit code.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:apiKeyId IS NULL OR a.apiKey.id = :apiKeyId)
              AND (:connectionId IS NULL OR a.databaseConnection.id = :connectionId)
              AND (:sourceType IS NULL OR a.sourceType = :sourceType)
              AND (:success IS NULL OR a.success = :success)
              AND (:operation IS NULL OR a.operation = :operation)
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> search(
            @Param("apiKeyId") Long apiKeyId,
            @Param("connectionId") Long connectionId,
            @Param("sourceType") AuditSourceType sourceType,
            @Param("success") Boolean success,
            @Param("operation") String operation,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    long countBySuccess(Boolean success);

    @Query("SELECT AVG(a.executionTimeMs) FROM AuditLog a")
    Double averageExecutionTimeMs();
}