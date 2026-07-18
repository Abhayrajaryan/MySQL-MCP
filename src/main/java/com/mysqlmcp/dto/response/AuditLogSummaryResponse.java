package com.mysqlmcp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLogSummaryResponse {
    private long totalRequests;
    private long allowedRequests;
    private long deniedOrFailedRequests;
    private Double averageExecutionTimeMs;
}