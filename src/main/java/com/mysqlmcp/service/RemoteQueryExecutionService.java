package com.mysqlmcp.service;

import com.mysqlmcp.config.QueryLimitsProperties;
import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.enums.DatabasePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Executes SQL against a target/remote database on behalf of an API key.
 * This is intentionally separate from {@link DatabaseConnectionService},
 * which manages this application's own DatabaseConnection/ApiKey records —
 * running arbitrary SQL against a dynamically-resolved external database is a
 * different responsibility and shouldn't live in the same class.
 *
 * <p>This is the single choke point every MCP tool call goes through, which
 * makes it the natural place to both authorize the request and record it to
 * the audit trail (see {@link AuditLogService}) with one consistent shape:
 * who (the resolved API key), what (the permission/operation and the SQL
 * text), whether it succeeded, and how long the whole thing — auth included
 * — took.
 */
@Service
@RequiredArgsConstructor
public class RemoteQueryExecutionService {

    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;
    private final ApiKeyAuthService apiKeyAuthService;
    private final AuditLogService auditLogService;
    private final QueryLimitsProperties queryLimits;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeShowTables(String rawApiKey) {
        return executeAndAudit(rawApiKey, DatabasePermission.SHOW_TABLES, "SHOW TABLES",
                jdbcTemplate -> jdbcTemplate.queryForList("SHOW TABLES"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeDescribeTable(String rawApiKey, String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("Table name must not be null or blank");
        }
        String query = "DESCRIBE " + tableName;
        return executeAndAudit(rawApiKey, DatabasePermission.DESCRIBE_TABLE, query,
                jdbcTemplate -> jdbcTemplate.queryForList(query));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeSelect(String rawApiKey, String query) {
        return executeAndAudit(rawApiKey, DatabasePermission.SELECT, query,
                jdbcTemplate -> jdbcTemplate.queryForList(query));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeExplain(String rawApiKey, String query) {
        String explainQuery = "EXPLAIN " + query;
        return executeAndAudit(rawApiKey, DatabasePermission.EXPLAIN, explainQuery,
                jdbcTemplate -> jdbcTemplate.queryForList(explainQuery));
    }

    /** Used for INSERT, UPDATE and DELETE — the permission passed in doubles as the audited operation name. */
    @Transactional
    public int executeWrite(String rawApiKey, DatabasePermission permission, String query) {
        validateStatementType(permission, query);
        return executeAndAudit(rawApiKey, permission, query, jdbcTemplate -> jdbcTemplate.update(query));
    }

    /** Used for CREATE_TABLE, ALTER_TABLE and DROP_TABLE. */
    @Transactional
    public void executeDdl(String rawApiKey, DatabasePermission permission, String query) {
        validateStatementType(permission, query);
        executeAndAudit(rawApiKey, permission, query, jdbcTemplate -> {
            jdbcTemplate.execute(query);
            return null;
        });
    }

    /**
     * Resolves and authorizes the API key, runs {@code action} against a
     * JdbcTemplate for its target connection, and always records the attempt
     * to the audit trail — success or failure, including auth failures.
     */
    private <T> T executeAndAudit(String rawApiKey, DatabasePermission permission, String query, SqlAction<T> action) {
        long startedAt = System.currentTimeMillis();
        ApiKey apiKey = null;
        try {
            apiKey = apiKeyAuthService.resolveApiKey(rawApiKey);
            apiKeyAuthService.validatePermission(apiKey, permission);
            validateQueryLength(query);

            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(apiKey.getDatabaseConnection());
            T result = action.run(jdbcTemplate);

            audit(apiKey, permission, query, true, null, System.currentTimeMillis() - startedAt);
            return result;
        } catch (RuntimeException ex) {
            audit(apiKey, permission, query, false, ex.getMessage(), System.currentTimeMillis() - startedAt);
            throw ex;
        }
    }

    private void audit(ApiKey apiKey, DatabasePermission permission, String query,
                       boolean success, String errorMessage, long elapsedMs) {
        Long apiKeyId = apiKey != null ? apiKey.getId() : null;
        Long connectionId = apiKey != null && apiKey.getDatabaseConnection() != null
                ? apiKey.getDatabaseConnection().getId() : null;
        auditLogService.record(apiKeyId, connectionId, permission.name(), query, success, errorMessage, elapsedMs);
    }

    /**
     * Lightweight check that the first keyword of the SQL matches the claimed
     * permission, so a key with only INSERT cannot sneak DROP TABLE through
     * {@code executeInsert}. This is not an SQL parser — it only looks at the
     * first non-whitespace token — but it catches the common bypass case.
     */
    private void validateStatementType(DatabasePermission permission, String sql) {
        if (sql == null || sql.isBlank()) return;

        String trimmed = sql.trim().toUpperCase();
        // Extract the first word (the statement keyword)
        String firstWord = trimmed.split("\\s+", 2)[0];

        // Determine what the first word should be for each permission
        String expectedKeyword = switch (permission) {
            case INSERT -> "INSERT";
            case UPDATE -> "UPDATE";
            case DELETE -> "DELETE";
            case CREATE_TABLE -> "CREATE";
            case ALTER_TABLE -> "ALTER";
            case DROP_TABLE -> "DROP";
            // SELECT, EXPLAIN, SHOW_TABLES, DESCRIBE_TABLE are read-only
            // and use a different code path that pre-pends the keyword.
            default -> null;
        };

        if (expectedKeyword != null && !firstWord.equals(expectedKeyword)) {
            throw new IllegalArgumentException(
                    "Permission " + permission + " does not allow " + firstWord + " statements. "
                            + "Expected keyword: " + expectedKeyword);
        }
    }

    private void validateQueryLength(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Query must not be null or blank");
        }
        if (input.length() > queryLimits.getMaxLength()) {
            throw new IllegalArgumentException(
                    "Query exceeds maximum allowed length of " + queryLimits.getMaxLength()
                            + " characters (was " + input.length() + ")");
        }
    }

    @FunctionalInterface
    private interface SqlAction<T> {
        T run(JdbcTemplate jdbcTemplate);
    }
}