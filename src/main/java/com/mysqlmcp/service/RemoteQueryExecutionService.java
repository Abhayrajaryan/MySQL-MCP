package com.mysqlmcp.service;

import com.mysqlmcp.config.QueryLimitsProperties;
import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Responsible for executing SQL against a target/remote database on behalf of
 * an API key. This is intentionally separate from {@link DatabaseConnectionService},
 * which manages this application's own DatabaseConnection/ApiKey records —
 * running arbitrary SQL against a dynamically-resolved external database is a
 * different responsibility and shouldn't live in the same class.
 *
 * <p>Every execution path here is bounded by the runtime protections in
 * {@link QueryLimitsProperties} (query length up front, timeout/max-rows via
 * {@link DynamicJdbcTemplateProvider}) and reports success/failure to the log
 * so operators have visibility into what ran, even before full DB-backed audit
 * logging exists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteQueryExecutionService {

    private final ApiKeyRepository apiKeyRepo;
    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;
    private final ApiKeyAuthService apiKeyAuthService;
    private final QueryLimitsProperties queryLimits;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeShowTables(String rawApiKey) {
        ApiKey apiKey = resolveApiKey(rawApiKey);
        return executeQuery(apiKey, "SHOW_TABLES", "SHOW TABLES");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeDescribeTable(String rawApiKey, String tableName) {
        ApiKey apiKey = resolveApiKey(rawApiKey);
        validateQueryLength(tableName);
        return executeQuery(apiKey, "DESCRIBE_TABLE", "DESCRIBE " + tableName);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeSelect(String rawApiKey, String query) {
        ApiKey apiKey = resolveApiKey(rawApiKey);
        validateQueryLength(query);
        return executeQuery(apiKey, "SELECT", query);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeExplain(String rawApiKey, String query) {
        ApiKey apiKey = resolveApiKey(rawApiKey);
        validateQueryLength(query);
        return executeQuery(apiKey, "EXPLAIN", "EXPLAIN " + query);
    }

    @Transactional
    public int executeUpdate(String rawApiKey, String query) {
        ApiKey apiKey = resolveApiKey(rawApiKey);
        validateQueryLength(query);
        long startedAt = System.currentTimeMillis();
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(apiKey.getDatabaseConnection());
            int affectedRows = jdbcTemplate.update(query);
            logSuccess(apiKey, "UPDATE", System.currentTimeMillis() - startedAt);
            return affectedRows;
        } catch (RuntimeException ex) {
            logFailure(apiKey, "UPDATE", System.currentTimeMillis() - startedAt, ex);
            throw ex;
        }
    }

    @Transactional
    public void executeDdl(String rawApiKey, String query) {
        ApiKey apiKey = resolveApiKey(rawApiKey);
        validateQueryLength(query);
        long startedAt = System.currentTimeMillis();
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(apiKey.getDatabaseConnection());
            jdbcTemplate.execute(query);
            logSuccess(apiKey, "DDL", System.currentTimeMillis() - startedAt);
        } catch (RuntimeException ex) {
            logFailure(apiKey, "DDL", System.currentTimeMillis() - startedAt, ex);
            throw ex;
        }
    }

    /**
     * Resolves the ApiKey (and its lazily-loaded DatabaseConnection) for a raw
     * API key. Must run inside the same transaction as the caller so the
     * DatabaseConnection proxy can be initialized.
     */
    private ApiKey resolveApiKey(String rawApiKey) {
        String keyHash = apiKeyAuthService.hashApiKey(rawApiKey);
        return apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));
    }

    private List<Map<String, Object>> executeQuery(ApiKey apiKey, String operation, String query) {
        long startedAt = System.currentTimeMillis();
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(apiKey.getDatabaseConnection());
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query);
            logSuccess(apiKey, operation, System.currentTimeMillis() - startedAt);
            return result;
        } catch (RuntimeException ex) {
            logFailure(apiKey, operation, System.currentTimeMillis() - startedAt, ex);
            throw ex;
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

    private void logSuccess(ApiKey apiKey, String operation, long elapsedMs) {
        log.info("Query executed successfully - apiKeyId={}, connectionId={}, operation={}, elapsedMs={}",
                apiKey.getId(), apiKey.getDatabaseConnection().getId(), operation, elapsedMs);
    }

    private void logFailure(ApiKey apiKey, String operation, long elapsedMs, Exception error) {
        log.warn("Query execution failed - apiKeyId={}, connectionId={}, operation={}, elapsedMs={}, error={}",
                apiKey.getId(), apiKey.getDatabaseConnection().getId(), operation, elapsedMs, error.getMessage());
    }
}