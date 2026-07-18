package com.mysqlmcp.service;

import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Responsible for executing SQL against a target/remote database on behalf of
 * an API key. This is intentionally separate from {@link DatabaseConnectionService},
 * which manages this application's own DatabaseConnection/ApiKey records —
 * running arbitrary SQL against a dynamically-resolved external database is a
 * different responsibility and shouldn't live in the same class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteQueryExecutionService {

    private final ApiKeyRepository apiKeyRepo;
    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeShowTables(String rawApiKey) {
        DatabaseConnection conn = resolveConnection(rawApiKey);
        return executeQuery(conn, "SHOW TABLES");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeDescribeTable(String rawApiKey, String tableName) {
        DatabaseConnection conn = resolveConnection(rawApiKey);
        return executeQuery(conn, "DESCRIBE " + tableName);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeSelect(String rawApiKey, String query) {
        DatabaseConnection conn = resolveConnection(rawApiKey);
        return executeQuery(conn, query);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> executeExplain(String rawApiKey, String query) {
        DatabaseConnection conn = resolveConnection(rawApiKey);
        return executeQuery(conn, "EXPLAIN " + query);
    }

    @Transactional
    public int executeUpdate(String rawApiKey, String query) {
        DatabaseConnection conn = resolveConnection(rawApiKey);
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(conn);
        return jdbcTemplate.update(query);
    }

    @Transactional
    public void executeDdl(String rawApiKey, String query) {
        DatabaseConnection conn = resolveConnection(rawApiKey);
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(conn);
        jdbcTemplate.execute(query);
    }

    /**
     * Resolves the target DatabaseConnection for a raw API key. Must run inside
     * the same transaction as the caller so the lazily-loaded DatabaseConnection
     * proxy on ApiKey can be initialized.
     */
    private DatabaseConnection resolveConnection(String rawApiKey) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        return apiKey.getDatabaseConnection();
    }

    private List<Map<String, Object>> executeQuery(DatabaseConnection conn, String query) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(conn);
        return jdbcTemplate.queryForList(query);
    }

    private String hashApiKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}