package com.mysqlmcp.dto;

import com.mysqlmcp.enums.DatabasePermission;

import java.util.Set;

public record DatabaseAccessContext(
        Long apiKeyId,
        Long databaseConnectionId,
        String host,
        Integer port,
        String databaseName,
        String dbUsername,
        String encryptedPassword,
        Set<DatabasePermission> permissions
) {
}