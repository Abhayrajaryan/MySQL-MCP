package com.mysqlmcp.dto.request;

import lombok.Data;

@Data
public class McpQueryRequest {
    private String apiKey;
    private String query;
}