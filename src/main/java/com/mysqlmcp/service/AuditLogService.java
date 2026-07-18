package com.mysqlmcp.service;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.AuditSourceType;
import com.mysqlmcp.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes {@link AuditLog} entries answering: who made the request, what they
 * tried to do, whether it was allowed, what happened, and how long it took.
 *
 * <p>Every write runs in its own {@code REQUIRES_NEW} transaction so an audit
 * entry survives even when the caller's own transaction (e.g. an UPDATE/DDL
 * execution) rolls back on failure. Persistence failures here are swallowed
 * and logged rather than propagated — a broken audit trail should never be
 * the reason a real request fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int MAX_TEXT_LENGTH = 4000;

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long apiKeyId, Long connectionId, AuditSourceType sourceType, String operation,
                              String requestPayload, String responseSummary, Long rowsAffected, long elapsedMs) {
        save(apiKeyId, connectionId, sourceType, operation, requestPayload, true,
                responseSummary, rowsAffected, elapsedMs, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long apiKeyId, Long connectionId, AuditSourceType sourceType, String operation,
                              String requestPayload, long elapsedMs, String errorCode, String errorMessage) {
        save(apiKeyId, connectionId, sourceType, operation, requestPayload, false,
                null, null, elapsedMs, errorCode, errorMessage);
    }

    private void save(Long apiKeyId, Long connectionId, AuditSourceType sourceType, String operation,
                      String requestPayload, boolean success, String responseSummary, Long rowsAffected,
                      long elapsedMs, String errorCode, String errorMessage) {
        try {
            AuditLog entry = new AuditLog();
            if (apiKeyId != null) {
                entry.setApiKey(entityManager.getReference(ApiKey.class, apiKeyId));
            }
            if (connectionId != null) {
                entry.setDatabaseConnection(entityManager.getReference(DatabaseConnection.class, connectionId));
            }
            entry.setSourceType(sourceType);
            entry.setOperation(operation);
            entry.setRequestPayload(truncate(requestPayload));
            entry.setSuccess(success);
            entry.setResponseSummary(truncate(responseSummary));
            entry.setRowsAffected(rowsAffected);
            entry.setExecutionTimeMs(elapsedMs);
            entry.setErrorCode(errorCode);
            entry.setErrorMessage(truncate(errorMessage));

            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Never let audit logging break the actual request/response path.
            log.error("Failed to persist audit log entry for operation '{}': {}", operation, e.getMessage(), e);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "...(truncated)" : text;
    }
}