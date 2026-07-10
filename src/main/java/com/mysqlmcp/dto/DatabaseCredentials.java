package com.mysqlmcp.dto;

public record DatabaseCredentials(
        String host,
        Integer port,
        String databaseName,
        String username,
        String password
) {
}