package com.mysqlmcp.service;

import com.mysqlmcp.dto.DatabaseAccessContext;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.ApiKeyPermission;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.exception.InvalidApiKeyException;
import com.mysqlmcp.exception.PermissionDeniedException;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApiKeyValidationService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository permissionRepository;

    public ApiKeyValidationService(ApiKeyRepository apiKeyRepository,
                                   ApiKeyPermissionRepository permissionRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.permissionRepository = permissionRepository;
    }

    public DatabaseAccessContext validateAndResolve(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new InvalidApiKeyException("API key is required");
        }

        String hash = hashApiKey(rawApiKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(hash)
                .orElseThrow(() -> new InvalidApiKeyException("Invalid API key"));

        if (!apiKey.getIsActive()) {
            throw new InvalidApiKeyException("API key is disabled");
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidApiKeyException("API key has expired");
        }

        DatabaseConnection connection = apiKey.getDatabaseConnection();
        if (!connection.getIsActive()) {
            throw new InvalidApiKeyException("Associated database connection is inactive");
        }

        Set<DatabasePermission> permissions = permissionRepository.findByApiKeyId(apiKey.getId())
                .stream()
                .map(ApiKeyPermission::getPermission)
                .collect(Collectors.toSet());

        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        return new DatabaseAccessContext(
                apiKey.getId(),
                connection.getId(),
                connection.getHost(),
                connection.getPort(),
                connection.getDatabaseName(),
                connection.getDbUsername(),
                connection.getEncryptedPassword(),
                permissions
        );
    }

    public void checkPermission(DatabaseAccessContext context, DatabasePermission required) {
        if (!context.permissions().contains(required)) {
            throw new PermissionDeniedException(
                    "This API key does not have permission to perform " + required + " operations"
            );
        }
    }

    public static String hashApiKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    public static String generateApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String keySuffix = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return "mcp_live_" + keySuffix;
    }

    public static String getKeyPrefix(String rawKey) {
        if (rawKey.length() <= 15) {
            return rawKey;
        }
        return rawKey.substring(0, 15) + "...";
    }
}