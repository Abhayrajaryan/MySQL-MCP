package com.mysqlmcp.service;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.AuditSourceType;
import com.mysqlmcp.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog record(AuditLogEntry entry) {
        AuditLog log = new AuditLog();
        log.setApiKey(entry.apiKey());
        log.setDatabaseConnection(entry.databaseConnection());
        log.setSourceType(entry.sourceType());
        log.setOperation(entry.operation());
        log.setRequestPayload(entry.requestPayload());
        log.setResponseSummary(entry.responseSummary());
        log.setSuccess(entry.success());
        log.setRowsAffected(entry.rowsAffected());
        log.setExecutionTimeMs(entry.executionTimeMs());
        log.setErrorCode(entry.errorCode());
        log.setErrorMessage(entry.errorMessage());
        return auditLogRepository.save(log);
    }

    public List<AuditLog> findByApiKeyId(Long apiKeyId) {
        return auditLogRepository.findByApiKeyIdOrderByCreatedAtDesc(apiKeyId);
    }

    public List<AuditLog> findByDatabaseConnectionId(Long databaseConnectionId) {
        return auditLogRepository.findByDatabaseConnectionIdOrderByCreatedAtDesc(databaseConnectionId);
    }

    public List<AuditLog> findAll() {
        return auditLogRepository.findAll();
    }

    public record AuditLogEntry(
            ApiKey apiKey,
            DatabaseConnection databaseConnection,
            AuditSourceType sourceType,
            String operation,
            String requestPayload,
            String responseSummary,
            Boolean success,
            Long rowsAffected,
            Long executionTimeMs,
            String errorCode,
            String errorMessage
    ) {}
}