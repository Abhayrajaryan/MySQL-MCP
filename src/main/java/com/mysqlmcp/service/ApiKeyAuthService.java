package com.mysqlmcp.service;

import com.mysqlmcp.config.SecurityDefaultsProperties;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * Service responsible for API key authentication and permission validation.
 * Centralizes auth-related logic to avoid duplication across the codebase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository permissionRepository;
    private final SecurityDefaultsProperties securityDefaults;

    /**
     * Validates an API key and checks if it has the required permission.
     *
     * @param rawApiKey The raw API key to validate
     * @param requiredPermission The permission required for the operation
     * @throws IllegalArgumentException if the API key is invalid, disabled, or lacks permission
     */
    public void validateApiKeyAndPermission(String rawApiKey, DatabasePermission requiredPermission) {
        String keyHash = hashApiKey(rawApiKey);

        ApiKey apiKey = apiKeyRepository.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));

        if (!apiKey.getIsActive()) {
            throw new IllegalArgumentException("API key is disabled");
        }

        boolean hasPermission = permissionRepository.findAll().stream()
                .anyMatch(p -> p.getApiKey().getId().equals(apiKey.getId())
                        && p.getPermission() == requiredPermission);

        if (!hasPermission) {
            throw new IllegalArgumentException(
                    "API key does not have " + requiredPermission + " permission");
        }

        enforceGlobalSecurityDefaults(requiredPermission);
    }

    /**
     * Applies the server-wide write/DDL kill-switch on top of the per-key
     * permission check above. A key can be granted a write or DDL permission
     * and still be refused here if the operator hasn't explicitly enabled
     * that class of operation server-wide.
     */
    private void enforceGlobalSecurityDefaults(DatabasePermission permission) {
        if (permission.isWriteOperation() && !securityDefaults.isWriteOperationsEnabled()) {
            throw new IllegalArgumentException(
                    "Write operations (INSERT/UPDATE/DELETE) are disabled by default on this server. "
                            + "Set mysql-mcp.security.enable-write-operations=true to allow them.");
        }
        if (permission.isDdlOperation() && !securityDefaults.isDdlOperationsEnabled()) {
            throw new IllegalArgumentException(
                    "DDL operations (CREATE/ALTER/DROP TABLE) are disabled by default on this server. "
                            + "Set mysql-mcp.security.enable-ddl-operations=true to allow them.");
        }
    }

    /**
     * Hashes an API key using SHA-256 and encodes it in Base64.
     *
     * @param rawKey The raw API key to hash
     * @return The Base64-encoded SHA-256 hash
     * @throws RuntimeException if hashing fails
     */
    public String hashApiKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}