package com.mysqlmcp.repository;

import com.mysqlmcp.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByApiKeyIdOrderByCreatedAtDesc(Long apiKeyId);
    List<AuditLog> findByDatabaseConnectionIdOrderByCreatedAtDesc(Long databaseConnectionId);
}