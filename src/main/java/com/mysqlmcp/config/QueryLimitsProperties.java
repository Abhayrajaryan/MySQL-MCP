package com.mysqlmcp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Runtime protections applied to every query executed against a target
 * database, regardless of API key permissions.
 */
@Getter
@Component
public class QueryLimitsProperties {

    /** Max seconds a single statement may run before the driver aborts it. */
    @Value("${mysql-mcp.query.timeout-seconds:10}")
    private int timeoutSeconds;

    /** Max rows a SELECT/SHOW/DESCRIBE/EXPLAIN statement may return. */
    @Value("${mysql-mcp.query.max-rows:1000}")
    private int maxRows;

    /** Max length, in characters, of a single incoming SQL statement. */
    @Value("${mysql-mcp.query.max-length:10000}")
    private int maxLength;
}