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
    private String apiKeyName;
    private String connectionName;
    private String operation;
    private String query;
    private Boolean success;
    private String errorMessage;
    private Long executionTimeMs;
    private LocalDateTime createdAt;
}