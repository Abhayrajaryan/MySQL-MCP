package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ConnectionDetailResponse {
    private Long id;
    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String dbUsername;
    private Boolean isActive;
    private String apiKeyPrefix;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
