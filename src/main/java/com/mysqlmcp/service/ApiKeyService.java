package com.mysqlmcp.service;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.entity.ApiKeyPermission;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.exception.DatabaseConnectionException;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository permissionRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         ApiKeyPermissionRepository permissionRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional
    public GeneratedApiKey createApiKey(Long databaseConnectionId, String name, Set<DatabasePermission> permissions,
                                        LocalDateTime expiresAt) {
        String rawKey = ApiKeyValidationService.generateApiKey();
        String hash = ApiKeyValidationService.hashApiKey(rawKey);
        String prefix = ApiKeyValidationService.getKeyPrefix(rawKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setKeyPrefix(prefix);
        apiKey.setKeyHash(hash);
        apiKey.setIsActive(true);
        apiKey.setExpiresAt(expiresAt);
        apiKey.setDatabaseConnection(null); // Will be set by caller who resolves the entity

        ApiKey saved = apiKeyRepository.save(apiKey);

        for (DatabasePermission permission : permissions) {
            ApiKeyPermission apiKeyPermission = new ApiKeyPermission();
            apiKeyPermission.setApiKey(saved);
            apiKeyPermission.setPermission(permission);
            permissionRepository.save(apiKeyPermission);
        }

        return new GeneratedApiKey(saved.getId(), rawKey, prefix, saved.getName(), saved.getIsActive(), saved.getExpiresAt());
    }

    public ApiKey createApiKeyEntity(ApiKey apiKey) {
        return apiKeyRepository.save(apiKey);
    }

    public List<ApiKey> findByDatabaseConnectionId(Long databaseConnectionId) {
        return apiKeyRepository.findByDatabaseConnectionId(databaseConnectionId);
    }

    public ApiKey findById(Long id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new DatabaseConnectionException("API key not found"));
    }

    public void delete(Long id) {
        ApiKey apiKey = findById(id);
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
    }

    public ApiKey toggleStatus(Long id, boolean active) {
        ApiKey apiKey = findById(id);
        apiKey.setIsActive(active);
        return apiKeyRepository.save(apiKey);
    }

    public record GeneratedApiKey(Long id, String rawKey, String prefix, String name, Boolean isActive, LocalDateTime expiresAt) {}
}