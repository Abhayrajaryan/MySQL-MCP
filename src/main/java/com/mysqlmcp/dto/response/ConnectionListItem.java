package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ConnectionListItem {
    private Long connectionId;
    private String name;
    private String host;
    private Integer port;
    private String databaseName;
    private String apiKeyPrefix;
    private Boolean active;
    private List<String> permissions;
}