package com.mysqlmcp.service;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.dto.request.UpsertDatabaseConnectionRequest;
import com.mysqlmcp.dto.response.ConnectionDetailResponse;
import com.mysqlmcp.dto.response.ConnectionListItem;
import com.mysqlmcp.dto.response.UpsertConnectionResponse;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.ApiKeyPermission;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.entity.User;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.repository.DatabaseConnectionRepository;
import com.mysqlmcp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseConnectionService {

    private static final String API_KEY_PREFIX = "mcp_live_";

    private final DatabaseConnectionRepository dbConnectionRepo;
    private final ApiKeyRepository apiKeyRepo;
    private final ApiKeyPermissionRepository apiKeyPermissionRepo;
    private final UserRepository userRepo;
    private final DatabaseCredentialEncryptor encryptor;

    @Transactional
    public UpsertConnectionResponse upsert(UpsertDatabaseConnectionRequest request) {
        boolean isCreate = (request.getId() == null);

        if (isCreate) {
            // Create new connection
            DatabaseConnection connection = new DatabaseConnection();
            connection.setUser(getDefaultUser());
            connection.setName(request.getName());
            connection.setHost(request.getHost());
            connection.setPort(request.getPort());
            connection.setDatabaseName(request.getDatabaseName());
            connection.setDbUsername(request.getDbUsername());
            connection.setEncryptedPassword(encryptor.encrypt(request.getPassword()));

            DatabaseConnection saved = dbConnectionRepo.save(connection);

            // Generate a new API key for this connection
            String rawKey = generateApiKey();
            String keyPrefix = rawKey.substring(0, Math.min(20, rawKey.length())) + "...";

            ApiKey apiKey = new ApiKey();
            apiKey.setDatabaseConnection(saved);
            apiKey.setName("default-key-" + saved.getId());
            apiKey.setKeyPrefix(keyPrefix);
            apiKey.setKeyHash(hashApiKey(rawKey));
            ApiKey savedKey = apiKeyRepo.save(apiKey);

            // Assign permissions based on request flags
            assignPermissions(savedKey, request);

            return UpsertConnectionResponse.builder()
                    .connectionId(saved.getId())
                    .connectionName(saved.getName())
                    .databaseName(saved.getDatabaseName())
                    .host(saved.getHost())
                    .port(saved.getPort())
                    .apiKey(rawKey)  // full key — returned only once
                    .created(true)
                    .build();
        } else {
            // Update existing connection
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

            // On update, replace permissions with the request flags
            apiKeyRepo.findAll().stream()
                    .filter(k -> k.getDatabaseConnection().getId().equals(saved.getId()))
                    .findFirst()
                    .ifPresent(existingKey -> {
                        // Remove old permissions
                        List<ApiKeyPermission> oldPermissions = apiKeyPermissionRepo.findAll().stream()
                                .filter(p -> p.getApiKey().getId().equals(existingKey.getId()))
                                .toList();
                        apiKeyPermissionRepo.deleteAll(oldPermissions);

                        // Assign new permissions from request flags
                        assignPermissions(existingKey, request);
                    });

            // Get existing key prefix (no new key generated on update)
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
            String keyPrefix = apiKeyRepo.findAll().stream()
                    .filter(k -> k.getDatabaseConnection().getId().equals(conn.getId()))
                    .findFirst()
                    .map(ApiKey::getKeyPrefix)
                    .orElse(null);

            return ConnectionListItem.builder()
                    .connectionId(conn.getId())
                    .apiKeyPrefix(keyPrefix)
                    .databaseName(conn.getDatabaseName())
                    .host(conn.getHost())
                    .port(conn.getPort())
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
                .isActive(conn.getIsActive())
                .apiKeyPrefix(apiKeyPrefix)
                .createdAt(conn.getCreatedAt())
                .updatedAt(conn.getUpdatedAt())
                .build();
    }

    /**
     * Reads the permission flags from the request and creates ApiKeyPermission records.
     */
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

    // --- Private helpers ---

    /**
     * Returns user with ID 1, creating it if it doesn't exist.
     * This is a temporary placeholder until authentication is implemented.
     */
    private User getDefaultUser() {
        return userRepo.findById(1L).orElseGet(() -> {
            User user = new User();
            user.setEmail("default@mysqlmcp.local");
            user.setPasswordHash("placeholder-hash");
            return userRepo.save(user);
        });
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return API_KEY_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Simple SHA-256 hash for API keys.
     * A more robust approach (bcrypt/scrypt) can replace this later.
     */
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