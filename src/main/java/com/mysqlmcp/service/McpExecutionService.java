package com.mysqlmcp.service;

import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class McpExecutionService {

    private final ApiKeyRepository apiKeyRepo;
    private final ApiKeyPermissionRepository apiKeyPermissionRepo;
    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;

    /**
     * Executes a query on behalf of an API key.
     * <p>
     * Flow: API key → hash → find key → check permission → resolve DB config →
     *       dynamic JdbcTemplate → execute query → return results
     */
    @Transactional
    public Object execute(String rawApiKey, String query) {
        // 1. Hash the provided API key and find matching record
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        if (!apiKey.getIsActive()) {
            throw new IllegalArgumentException("API key is disabled");
        }

        // 2. Determine required permission from query type
        DatabasePermission requiredPermission = determinePermission(query);
        if (requiredPermission == null) {
            throw new IllegalArgumentException("Unsupported query type");
        }

        // 3. Check permission
        boolean hasPermission = apiKeyPermissionRepo.findAll().stream()
                .anyMatch(p -> p.getApiKey().getId().equals(apiKey.getId())
                        && p.getPermission() == requiredPermission);
        if (!hasPermission) {
            throw new IllegalArgumentException(
                    "API key does not have " + requiredPermission + " permission");
        }

        // 4. Resolve the database connection
        DatabaseConnection connection = apiKey.getDatabaseConnection();

        // 5. Create dynamic JdbcTemplate
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(connection);

        // 6. Execute the query
        String trimmedQuery = query.trim().toUpperCase();

        if (trimmedQuery.startsWith("SELECT") || trimmedQuery.startsWith("SHOW")) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
            return Map.of(
                    "rows", rows,
                    "count", rows.size()
            );
        } else if (trimmedQuery.startsWith("DESCRIBE") || trimmedQuery.startsWith("EXPLAIN")) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
            return Map.of(
                    "columns", rows
            );
        } else if (trimmedQuery.startsWith("INSERT") || trimmedQuery.startsWith("UPDATE") || trimmedQuery.startsWith("DELETE")) {
            int affected = jdbcTemplate.update(query);
            return Map.of("affectedRows", affected);
        } else {
            // DDL or other — use execute
            jdbcTemplate.execute(query);
            return Map.of("message", "Query executed successfully");
        }
    }

    /**
     * Determines which DatabasePermission is required for a given query.
     */
    private DatabasePermission determinePermission(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        String trimmed = query.trim().toUpperCase();

        if (trimmed.startsWith("SHOW TABLES")) {
            return DatabasePermission.SHOW_TABLES;
        }
        if (trimmed.startsWith("SHOW")) {
            return DatabasePermission.SHOW_TABLES;
        }
        if (trimmed.startsWith("DESCRIBE") || trimmed.startsWith("DESC")) {
            return DatabasePermission.DESCRIBE_TABLE;
        }
        if (trimmed.startsWith("EXPLAIN")) {
            return DatabasePermission.EXPLAIN;
        }
        if (trimmed.startsWith("SELECT")) {
            return DatabasePermission.SELECT;
        }
        if (trimmed.startsWith("INSERT")) {
            return DatabasePermission.INSERT;
        }
        if (trimmed.startsWith("UPDATE")) {
            return DatabasePermission.UPDATE;
        }
        if (trimmed.startsWith("DELETE")) {
            return DatabasePermission.DELETE;
        }
        if (trimmed.startsWith("CREATE TABLE") || trimmed.startsWith("CREATE")) {
            return DatabasePermission.CREATE_TABLE;
        }
        if (trimmed.startsWith("ALTER")) {
            return DatabasePermission.ALTER_TABLE;
        }
        if (trimmed.startsWith("DROP")) {
            return DatabasePermission.DROP_TABLE;
        }

        return null;
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