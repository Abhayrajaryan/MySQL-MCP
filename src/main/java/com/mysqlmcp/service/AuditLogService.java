package com.mysqlmcp.service;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes {@link AuditLog} entries answering: who made the request, what they
 * tried to do, and how long it took.
 *
 * <p>Runs in its own {@code REQUIRES_NEW} transaction so an audit entry
 * survives even when the caller's own transaction (e.g. an UPDATE/DDL
 * execution) rolls back on failure. Persistence failures here are swallowed
 * and logged rather than propagated — a broken audit trail should never be
 * the reason a real request fails.
 *
 * <p>Takes IDs rather than entities: the entity may have been loaded in the
 * caller's transaction, which could already be rolling back by the time this
 * runs in a fresh one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int MAX_QUERY_LENGTH = 2000;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final AuditLogRepository auditLogRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long apiKeyId, Long connectionId, String operation, String query,
                       boolean success, String errorMessage, long elapsedMs) {
        try {
            AuditLog entry = new AuditLog();
            if (apiKeyId != null) {
                entry.setApiKey(entityManager.getReference(ApiKey.class, apiKeyId));
            }
            if (connectionId != null) {
                entry.setDatabaseConnection(entityManager.getReference(DatabaseConnection.class, connectionId));
            }
            entry.setOperation(operation);
            entry.setQuery(truncate(query, MAX_QUERY_LENGTH));
            entry.setSuccess(success);
            entry.setErrorMessage(truncate(errorMessage, MAX_ERROR_LENGTH));
            entry.setExecutionTimeMs(elapsedMs);

            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Never let audit logging break the actual request/response path.
            log.error("Failed to persist audit log entry for operation '{}': {}", operation, e.getMessage(), e);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "...(truncated)" : text;
    }
}