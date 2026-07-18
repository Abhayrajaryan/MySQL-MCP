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
     * Resolves the raw API key to its entity. Split out from permission
     * checking so callers (namely {@code RemoteQueryExecutionService}) can
     * still identify "who" made a request in the audit trail even when the
     * request is subsequently denied for lacking a permission.
     *
     * @throws IllegalArgumentException if no active key matches
     */
    public ApiKey resolveApiKey(String rawApiKey) {
        String keyHash = hashApiKey(rawApiKey);
        return apiKeyRepository.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid API key"));
    }

    /**
     * Checks whether an already-resolved API key is allowed to perform the
     * given operation: active, holds the specific permission, and the
     * operation's class (write/DDL) isn't disabled server-wide.
     *
     * @throws IllegalArgumentException if the key is disabled or lacks permission
     */
    public void validatePermission(ApiKey apiKey, DatabasePermission requiredPermission) {
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