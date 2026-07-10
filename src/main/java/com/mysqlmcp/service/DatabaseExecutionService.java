package com.mysqlmcp.service;

import com.mysqlmcp.config.MysqlMcpProperties;
import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.dto.DatabaseCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatabaseExecutionService {

    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;
    private final MysqlMcpProperties properties;

    public ShowTablesResult showTables(DatabaseCredentials credentials) {
        JdbcTemplate jdbcTemplate = createTemplate(credentials);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW TABLES");
        List<String> tables = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            tables.add(row.values().iterator().next().toString());
        }
        return new ShowTablesResult(tables, tables.size());
    }

    public DescribeTableResult describeTable(DatabaseCredentials credentials, String tableName) {
        JdbcTemplate jdbcTemplate = createTemplate(credentials);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("DESCRIBE " + tableName);
        List<ColumnInfo> columns = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("Field");
            String type = (String) row.get("Type");
            String nullable = (String) row.get("Null");
            String key = (String) row.get("Key");
            columns.add(new ColumnInfo(name, type, "YES".equals(nullable), "PRI".equals(key)));
        }
        return new DescribeTableResult(tableName, columns);
    }

    public SelectResult executeSelect(DatabaseCredentials credentials, String query) {
        JdbcTemplate jdbcTemplate = createTemplate(credentials);
        int maxRows = properties.getExecution().getMaxRows();
        jdbcTemplate.setMaxRows(maxRows + 1);

        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
        long executionTimeMs = System.currentTimeMillis() - startTime;

        boolean truncated = rows.size() > maxRows;
        if (truncated) {
            rows = rows.subList(0, maxRows);
        }

        List<String> columns = new ArrayList<>();
        if (!rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }

        return new SelectResult(columns, rows, rows.size(), truncated, executionTimeMs);
    }

    public ExplainResult explainQuery(DatabaseCredentials credentials, String query) {
        JdbcTemplate jdbcTemplate = createTemplate(credentials);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("EXPLAIN " + query);
        return new ExplainResult(rows);
    }

    private JdbcTemplate createTemplate(DatabaseCredentials credentials) {
        return jdbcTemplateProvider.createJdbcTemplate(
                credentials.host(),
                credentials.port(),
                credentials.databaseName(),
                credentials.username(),
                credentials.password()
        );
    }

    public record ShowTablesResult(List<String> tables, int count) {}
    public record ColumnInfo(String name, String type, boolean nullable, boolean primaryKey) {}
    public record DescribeTableResult(String tableName, List<ColumnInfo> columns) {}
    public record SelectResult(List<String> columns, List<Map<String, Object>> rows, int returnedRows, boolean truncated, long executionTimeMs) {}
    public record ExplainResult(List<Map<String, Object>> details) {}
}