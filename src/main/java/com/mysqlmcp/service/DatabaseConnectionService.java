package com.mysqlmcp.service;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.dto.request.UpsertDatabaseConnectionRequest;
import com.mysqlmcp.dto.response.ConnectionDetailResponse;
import com.mysqlmcp.dto.response.ConnectionListItem;
import com.mysqlmcp.dto.response.UpsertConnectionResponse;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.repository.DatabaseConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {

    private static final String API_KEY_PREFIX = "mcp_live_";

    private final DatabaseConnectionRepository dbConnectionRepo;
    private final ApiKeyRepository apiKeyRepo;
    private final ApiKeyPermissionRepository apiKeyPermissionRepo;
    private final DatabaseCredentialEncryptor encryptor;
    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;

    @Transactional
    public UpsertConnectionResponse upsert(UpsertDatabaseConnectionRequest request) {
        boolean isCreate = (request.getId() == null);

        if (isCreate) {
            log.info("Creating new database connection: {}", request.getName());
            DatabaseConnection connection = new DatabaseConnection();
            connection.setName(request.getName());
            connection.setHost(request.getHost());
            connection.setPort(request.getPort());
            connection.setDatabaseName(request.getDatabaseName());
            connection.setDbUsername(request.getDbUsername());
            connection.setEncryptedPassword(encryptor.encrypt(request.getPassword()));

            DatabaseConnection saved = dbConnectionRepo.save(connection);
            log.info("Database connection created with id: {}", saved.getId());

            return UpsertConnectionResponse.builder()
                    .connectionId(saved.getId())
                    .connectionName(saved.getName())
                    .databaseName(saved.getDatabaseName())
                    .host(saved.getHost())
                    .port(saved.getPort())
                    .created(true)
                    .build();
        } else {
            log.info("Updating database connection with id: {}", request.getId());
            DatabaseConnection existing = dbConnectionRepo.findById(request.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "DatabaseConnection not found with id: " + request.getId()));

            existing.setName(request.getName());
            existing.setHost(request.getHost());
            existing.setPort(request.getPort());
            existing.setDatabaseName(request.getDatabaseName());
            existing.setDbUsername(request.getDbUsername());
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                existing.setEncryptedPassword(encryptor.encrypt(request.getPassword()));
            }

            DatabaseConnection saved = dbConnectionRepo.save(existing);
            log.info("Database connection updated with id: {}", saved.getId());

            return UpsertConnectionResponse.builder()
                    .connectionId(saved.getId())
                    .connectionName(saved.getName())
                    .databaseName(saved.getDatabaseName())
                    .host(saved.getHost())
                    .port(saved.getPort())
                    .created(false)
                    .build();
        }
    }

    public List<ConnectionListItem> getAll() {
        List<DatabaseConnection> connections = dbConnectionRepo.findAll();

        return connections.stream().map(conn -> {
            List<ApiKey> apiKeys = apiKeyRepo.findAll().stream()
                    .filter(k -> k.getDatabaseConnection().getId().equals(conn.getId()))
                    .toList();

            List<String> keyPrefixes = apiKeys.stream()
                    .map(ApiKey::getKeyPrefix)
                    .toList();

            return ConnectionListItem.builder()
                    .connectionId(conn.getId())
                    .name(conn.getName())
                    .host(conn.getHost())
                    .port(conn.getPort())
                    .databaseName(conn.getDatabaseName())
                    .apiKeyPrefix(keyPrefixes.isEmpty() ? null : keyPrefixes.get(0))
                    .active(conn.getIsActive())
                    .permissions(List.of())
                    .build();
        }).toList();
    }

    public ConnectionDetailResponse findById(Long id) {
        DatabaseConnection conn = dbConnectionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DatabaseConnection not found with id: " + id));

        String apiKeyPrefix = apiKeyRepo.findAll().stream()
                .filter(k -> k.getDatabaseConnection().getId().equals(conn.getId()))
                .findFirst()
                .map(ApiKey::getKeyPrefix)
                .orElse(null);

        return ConnectionDetailResponse.builder()
                .id(conn.getId())
                .name(conn.getName())
                .host(conn.getHost())
                .port(conn.getPort())
                .databaseName(conn.getDatabaseName())
                .dbUsername(conn.getDbUsername())
                .isActive(conn.getIsActive())
                .apiKeyPrefix(apiKeyPrefix)
                .createdAt(conn.getCreatedAt())
                .updatedAt(conn.getUpdatedAt())
                .build();
    }

    public List<Map<String, Object>> getApiKeysWithPermissions(Long connectionId) {
        List<ApiKey> apiKeys = apiKeyRepo.findAll().stream()
                .filter(k -> k.getDatabaseConnection().getId().equals(connectionId))
                .toList();

        return apiKeys.stream().map(apiKey -> {
            List<String> permissions = apiKeyPermissionRepo.findAll().stream()
                    .filter(p -> p.getApiKey().getId().equals(apiKey.getId()))
                    .map(p -> p.getPermission().name())
                    .toList();

            return Map.of(
                    "id", apiKey.getId(),
                    "name", apiKey.getName(),
                    "keyPrefix", apiKey.getKeyPrefix(),
                    "permissions", permissions
            );
        }).toList();
    }

    public String generateApiKeyForConnection(Long connectionId, String keyName) {
        log.info("Generating new API key for connection id: {}, name: {}", connectionId, keyName);
        DatabaseConnection conn = dbConnectionRepo.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("DatabaseConnection not found with id: " + connectionId));

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawKey = API_KEY_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(randomBytes);

        ApiKey apiKey = new ApiKey();
        apiKey.setDatabaseConnection(conn);
        apiKey.setName(keyName);
        apiKey.setKeyPrefix(rawKey.substring(0, Math.min(20, rawKey.length())) + "...");
        apiKey.setKeyHash(hashApiKey(rawKey));
        apiKeyRepo.save(apiKey);

        log.info("API key generated successfully for connection id: {}", connectionId);
        return rawKey;
    }

    public List<Map<String, Object>> executeShowTables(String rawApiKey) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        DatabaseConnection conn = apiKey.getDatabaseConnection();
        return executeQuery(conn, "SHOW TABLES");
    }

    public List<Map<String, Object>> executeDescribeTable(String rawApiKey, String tableName) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        DatabaseConnection conn = apiKey.getDatabaseConnection();
        return executeQuery(conn, "DESCRIBE " + tableName);
    }

    public List<Map<String, Object>> executeSelect(String rawApiKey, String query) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        DatabaseConnection conn = apiKey.getDatabaseConnection();
        return executeQuery(conn, query);
    }

    public List<Map<String, Object>> executeExplain(String rawApiKey, String query) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        DatabaseConnection conn = apiKey.getDatabaseConnection();
        return executeQuery(conn, "EXPLAIN " + query);
    }

    public int executeUpdate(String rawApiKey, String query) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        DatabaseConnection conn = apiKey.getDatabaseConnection();
        return executeUpdate(conn, query);
    }

    public void executeDdl(String rawApiKey, String query) {
        String keyHash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        DatabaseConnection conn = apiKey.getDatabaseConnection();
        executeDdl(conn, query);
    }

    private List<Map<String, Object>> executeQuery(DatabaseConnection conn, String query) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(conn);
        return jdbcTemplate.queryForList(query);
    }

    private int executeUpdate(DatabaseConnection conn, String query) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(conn);
        return jdbcTemplate.update(query);
    }

    private void executeDdl(DatabaseConnection conn, String query) {
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(conn);
        jdbcTemplate.execute(query);
    }

    private String hashApiKey(String rawKey) {
        try {
            java.security.MessageDigest digest =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
