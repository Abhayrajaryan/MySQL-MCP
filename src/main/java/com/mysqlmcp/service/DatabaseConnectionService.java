package com.mysqlmcp.service;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.dto.request.UpsertDatabaseConnectionRequest;
import com.mysqlmcp.dto.response.ConnectionDetailResponse;
import com.mysqlmcp.dto.response.ConnectionListItem;
import com.mysqlmcp.dto.response.UpsertConnectionResponse;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.ApiKeyPermission;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.repository.DatabaseConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {

    private static final String API_KEY_PREFIX = "mcp_live_";

    private final DatabaseConnectionRepository dbConnectionRepo;
    private final ApiKeyRepository apiKeyRepo;
    private final ApiKeyPermissionRepository apiKeyPermissionRepo;
    private final DatabaseCredentialEncryptor encryptor;

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

            String rawKey = generateApiKey();
            String keyPrefix = rawKey.substring(0, Math.min(20, rawKey.length())) + "...";

            ApiKey apiKey = new ApiKey();
            apiKey.setDatabaseConnection(saved);
            apiKey.setName("default-key-" + saved.getId());
            apiKey.setKeyPrefix(keyPrefix);
            apiKey.setKeyHash(hashApiKey(rawKey));
            ApiKey savedKey = apiKeyRepo.save(apiKey);
            log.info("Default API key generated for connection id: {}", saved.getId());

            assignPermissions(savedKey, request);

            return UpsertConnectionResponse.builder()
                    .connectionId(saved.getId())
                    .connectionName(saved.getName())
                    .databaseName(saved.getDatabaseName())
                    .host(saved.getHost())
                    .port(saved.getPort())
                    .apiKey(rawKey)
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

            apiKeyRepo.findAll().stream()
                    .filter(k -> k.getDatabaseConnection().getId().equals(saved.getId()))
                    .findFirst()
                    .ifPresent(existingKey -> {
                        List<ApiKeyPermission> oldPermissions = apiKeyPermissionRepo.findAll().stream()
                                .filter(p -> p.getApiKey().getId().equals(existingKey.getId()))
                                .toList();
                        apiKeyPermissionRepo.deleteAll(oldPermissions);
                        assignPermissions(existingKey, request);
                    });

            String keyPrefix = apiKeyRepo.findAll().stream()
                    .filter(k -> k.getDatabaseConnection().getId().equals(saved.getId()))
                    .findFirst()
                    .map(ApiKey::getKeyPrefix)
                    .orElse("N/A");

            return UpsertConnectionResponse.builder()
                    .connectionId(saved.getId())
                    .connectionName(saved.getName())
                    .databaseName(saved.getDatabaseName())
                    .host(saved.getHost())
                    .port(saved.getPort())
                    .apiKey(keyPrefix)
                    .created(false)
                    .build();
        }
    }

    public List<ConnectionListItem> getAll() {
        List<DatabaseConnection> connections = dbConnectionRepo.findAll();

        return connections.stream().map(conn -> {
            ApiKey apiKey = apiKeyRepo.findAll().stream()
                    .filter(k -> k.getDatabaseConnection().getId().equals(conn.getId()))
                    .findFirst()
                    .orElse(null);

            List<String> permissions = new ArrayList<>();
            if (apiKey != null) {
                permissions = apiKeyPermissionRepo.findAll().stream()
                        .filter(p -> p.getApiKey().getId().equals(apiKey.getId()))
                        .map(p -> p.getPermission().name())
                        .toList();
            }

            return ConnectionListItem.builder()
                    .connectionId(conn.getId())
                    .name(conn.getName())
                    .host(conn.getHost())
                    .port(conn.getPort())
                    .databaseName(conn.getDatabaseName())
                    .apiKeyPrefix(apiKey != null ? apiKey.getKeyPrefix() : null)
                    .active(conn.getIsActive())
                    .permissions(permissions)
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

    private void assignPermissions(ApiKey apiKey, UpsertDatabaseConnectionRequest request) {
        List<ApiKeyPermission> permissions = new ArrayList<>();

        if (request.isShowTables())    permissions.add(buildPermission(apiKey, DatabasePermission.SHOW_TABLES));
        if (request.isDescribeTable()) permissions.add(buildPermission(apiKey, DatabasePermission.DESCRIBE_TABLE));
        if (request.isSelect())        permissions.add(buildPermission(apiKey, DatabasePermission.SELECT));
        if (request.isExplain())       permissions.add(buildPermission(apiKey, DatabasePermission.EXPLAIN));
        if (request.isInsert())        permissions.add(buildPermission(apiKey, DatabasePermission.INSERT));
        if (request.isUpdate())        permissions.add(buildPermission(apiKey, DatabasePermission.UPDATE));
        if (request.isDelete())        permissions.add(buildPermission(apiKey, DatabasePermission.DELETE));
        if (request.isCreateTable())   permissions.add(buildPermission(apiKey, DatabasePermission.CREATE_TABLE));
        if (request.isAlterTable())    permissions.add(buildPermission(apiKey, DatabasePermission.ALTER_TABLE));
        if (request.isDropTable())     permissions.add(buildPermission(apiKey, DatabasePermission.DROP_TABLE));

        apiKeyPermissionRepo.saveAll(permissions);
    }

    private ApiKeyPermission buildPermission(ApiKey apiKey, DatabasePermission permission) {
        ApiKeyPermission p = new ApiKeyPermission();
        p.setApiKey(apiKey);
        p.setPermission(permission);
        return p;
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

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return API_KEY_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(randomBytes);
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