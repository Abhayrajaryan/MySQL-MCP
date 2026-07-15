package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UpsertConnectionResponse {
    private Long connectionId;
    private String connectionName;
    private String databaseName;
    private String host;
    private Integer port;
    private boolean created;       // true = new, false = updated
}