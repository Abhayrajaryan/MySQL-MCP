package com.mysqlmcp.service;

import com.mysqlmcp.dto.response.AuditLogFilterOptionsResponse;
import com.mysqlmcp.dto.response.AuditLogResponse;
import com.mysqlmcp.dto.response.AuditLogSummaryResponse;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.AuditSourceType;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.repository.AuditLogRepository;
import com.mysqlmcp.repository.DatabaseConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final ApiKeyRepository apiKeyRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(Long apiKeyId, Long connectionId, AuditSourceType sourceType,
                                         Boolean success, String operation,
                                         LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository
                .search(apiKeyId, connectionId, sourceType, success, operation, from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AuditLogSummaryResponse summary() {
        long allowed = auditLogRepository.countBySuccess(true);
        long deniedOrFailed = auditLogRepository.countBySuccess(false);
        Double avg = auditLogRepository.averageExecutionTimeMs();

        return AuditLogSummaryResponse.builder()
                .totalRequests(allowed + deniedOrFailed)
                .allowedRequests(allowed)
                .deniedOrFailedRequests(deniedOrFailed)
                .averageExecutionTimeMs(avg)
                .build();
    }

    @Transactional(readOnly = true)
    public AuditLogFilterOptionsResponse filterOptions() {
        List<AuditLogFilterOptionsResponse.Option> connections = databaseConnectionRepository.findAll().stream()
                .map(c -> AuditLogFilterOptionsResponse.Option.builder().id(c.getId()).name(c.getName()).build())
                .toList();

        List<AuditLogFilterOptionsResponse.ApiKeyOption> apiKeys = apiKeyRepository.findAll().stream()
                .map(k -> AuditLogFilterOptionsResponse.ApiKeyOption.builder()
                        .id(k.getId())
                        .name(k.getName())
                        .connectionId(k.getDatabaseConnection() != null ? k.getDatabaseConnection().getId() : null)
                        .build())
                .toList();

        return AuditLogFilterOptionsResponse.builder().connections(connections).apiKeys(apiKeys).build();
    }

    private AuditLogResponse toResponse(AuditLog entry) {
        ApiKey apiKey = entry.getApiKey();
        DatabaseConnection connection = entry.getDatabaseConnection();

        return AuditLogResponse.builder()
                .id(entry.getId())
                .apiKeyId(apiKey != null ? apiKey.getId() : null)
                .apiKeyName(apiKey != null ? apiKey.getName() : null)
                .apiKeyPrefix(apiKey != null ? apiKey.getKeyPrefix() : null)
                .connectionId(connection != null ? connection.getId() : null)
                .connectionName(connection != null ? connection.getName() : null)
                .sourceType(entry.getSourceType() != null ? entry.getSourceType().name() : null)
                .operation(entry.getOperation())
                .requestPayload(entry.getRequestPayload())
                .success(entry.getSuccess())
                .responseSummary(entry.getResponseSummary())
                .rowsAffected(entry.getRowsAffected())
                .executionTimeMs(entry.getExecutionTimeMs())
                .errorCode(entry.getErrorCode())
                .errorMessage(entry.getErrorMessage())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}