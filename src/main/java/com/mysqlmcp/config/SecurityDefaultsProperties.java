package com.mysqlmcp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Global "master switch" security defaults for the server.
 * <p>
 * These sit above the per-API-key permission model: even if an API key has
 * been granted a write or DDL permission, that permission cannot be exercised
 * unless the corresponding flag here is also enabled. Both default to
 * {@code false} so a freshly-installed server is read-only until an operator
 * makes a deliberate choice to allow more.
 */
@Getter
@Component
public class SecurityDefaultsProperties {

    @Value("${mysql-mcp.security.enable-write-operations:false}")
    private boolean writeOperationsEnabled;

    @Value("${mysql-mcp.security.enable-ddl-operations:false}")
    private boolean ddlOperationsEnabled;
}
