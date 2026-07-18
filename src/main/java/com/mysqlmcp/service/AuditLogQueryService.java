package com.mysqlmcp.service;

import com.mysqlmcp.dto.response.AuditLogFilterOptionsResponse;
import com.mysqlmcp.dto.response.AuditLogResponse;
import com.mysqlmcp.dto.response.AuditLogSummaryResponse;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.AuditLogRepository;
import com.mysqlmcp.repository.DatabaseConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Read side of the audit trail: powers the "who requested what" dashboard tab.
 * Kept separate from {@link AuditLogService} (the writer) since the two have
 * very different transactional needs — this one is read-only and needs an
 * open session to safely resolve the lazy apiKey/databaseConnection names for
 * display.
 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final DatabaseConnectionRepository databaseConnectionRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(Long connectionId, String operation,
                                         LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.search(connectionId, operation, from, to, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogSummaryResponse summary() {
        return AuditLogSummaryResponse.builder()
                .totalRequests(auditLogRepository.count())
                .averageExecutionTimeMs(auditLogRepository.averageExecutionTimeMs())
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogFilterOptionsResponse filterOptions() {
        List<AuditLogFilterOptionsResponse.Option> connections = databaseConnectionRepository.findAll().stream()
                .map(c -> AuditLogFilterOptionsResponse.Option.builder().id(c.getId()).name(c.getName()).build())
                .toList();

        List<String> operations = Arrays.stream(DatabasePermission.values()).map(Enum::name).toList();

        return AuditLogFilterOptionsResponse.builder().connections(connections).operations(operations).build();
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        ApiKey apiKey = entry.getApiKey();
        DatabaseConnection connection = entry.getDatabaseConnection();

        return AuditLogResponse.builder()
                .id(entry.getId())
                .apiKeyName(apiKey != null ? apiKey.getName() : null)
                .connectionName(connection != null ? connection.getName() : null)
                .operation(entry.getOperation())
                .query(entry.getQuery())
                .success(entry.getSuccess())
                .errorMessage(entry.getErrorMessage())
                .executionTimeMs(entry.getExecutionTimeMs())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}