package com.mysqlmcp.database;

import org.springframework.stereotype.Component;

@Component
public class JdbcUrlBuilder {

    public String buildUrl(String host, Integer port, String databaseName) {
        return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                host, port, databaseName);
    }
}