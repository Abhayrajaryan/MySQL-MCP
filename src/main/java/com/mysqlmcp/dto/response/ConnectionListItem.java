package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ConnectionListItem {
    private Long connectionId;
    private String apiKeyPrefix;   // prefix of the first API key (e.g. "mcp_live_abc...")
    private String databaseName;
    private String host;
    private Integer port;
}