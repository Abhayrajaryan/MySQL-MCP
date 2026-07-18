package com.mysqlmcp.mcp;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.service.RemoteQueryExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class DatabaseModificationTools {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository permissionRepository;
    private final RemoteQueryExecutionService remoteQueryExecutionService;

    @Tool(description = "Execute an INSERT query. Requires INSERT permission. Returns affected rows count in CSV format.")
    public String executeInsert(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "INSERT query to execute") String query) {

        validateApiKeyAndPermission(apiKey, DatabasePermission.INSERT);

        int affectedRows = remoteQueryExecutionService.executeUpdate(apiKey, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute an UPDATE query. Requires UPDATE permission. Returns affected rows count in CSV format.")
    public String executeUpdate(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "UPDATE query to execute") String query) {

        validateApiKeyAndPermission(apiKey, DatabasePermission.UPDATE);

        int affectedRows = remoteQueryExecutionService.executeUpdate(apiKey, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute a DELETE query. Requires DELETE permission. Returns affected rows count in CSV format.")
    public String executeDelete(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "DELETE query to execute") String query) {

        validateApiKeyAndPermission(apiKey, DatabasePermission.DELETE);

        int affectedRows = remoteQueryExecutionService.executeUpdate(apiKey, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute a CREATE TABLE query. Requires CREATE_TABLE permission. Returns success message in CSV format.")
    public String createTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "CREATE TABLE query to execute") String query) {

        validateApiKeyAndPermission(apiKey, DatabasePermission.CREATE_TABLE);

        remoteQueryExecutionService.executeDdl(apiKey, query);
        return "success,message,Table created successfully\n";
    }

    @Tool(description = "Execute an ALTER TABLE query. Requires ALTER_TABLE permission. Returns success message in CSV format.")
    public String alterTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "ALTER TABLE query to execute") String query) {

        validateApiKeyAndPermission(apiKey, DatabasePermission.ALTER_TABLE);

        remoteQueryExecutionService.executeDdl(apiKey, query);
        return "success,message,Table altered successfully\n";
    }

    @Tool(description = "Execute a DROP TABLE query. Requires DROP_TABLE permission. Returns success message in CSV format.")
    public String dropTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "DROP TABLE query to execute") String query) {

        validateApiKeyAndPermission(apiKey, DatabasePermission.DROP_TABLE);

        remoteQueryExecutionService.executeDdl(apiKey, query);
        return "success,message,Table dropped successfully\n";
    }

    private void validateApiKeyAndPermission(String rawApiKey, DatabasePermission requiredPermission) {
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