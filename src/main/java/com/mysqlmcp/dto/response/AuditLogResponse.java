package com.mysqlmcp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {
    private Long id;
    private Long apiKeyId;
    private String apiKeyName;
    private String apiKeyPrefix;
    private Long connectionId;
    private String connectionName;
    private String sourceType;
    private String operation;
    private String requestPayload;
    private Boolean success;
    private String responseSummary;
    private Long rowsAffected;
    private Long executionTimeMs;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
}