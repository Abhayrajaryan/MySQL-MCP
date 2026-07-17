package com.mysqlmcp.mcp;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.service.DatabaseConnectionService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseMetadataTools {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyPermissionRepository permissionRepository;
    private final DatabaseConnectionService dbConnectionService;

    public DatabaseMetadataTools(ApiKeyRepository apiKeyRepository,
                                 ApiKeyPermissionRepository permissionRepository,
                                 DatabaseConnectionService dbConnectionService) {
        this.apiKeyRepository = apiKeyRepository;
        this.permissionRepository = permissionRepository;
        this.dbConnectionService = dbConnectionService;
    }

    @Tool(description = "Show all tables in the database. Requires SHOW_TABLES permission.")
    public String showTables(
            @ToolParam(description = "API key for authentication") String apiKey) {
        
        validateApiKeyAndPermission(apiKey, DatabasePermission.SHOW_TABLES);
        
        // This will be implemented to return CSV format
        List<Map<String, Object>> result = dbConnectionService.executeShowTables(apiKey);
        return convertToCsv(result);
    }

    @Tool(description = "Describe the structure of a table. Requires DESCRIBE_TABLE permission.")
    public String describeTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "Name of the table to describe") String tableName) {
        
        validateApiKeyAndPermission(apiKey, DatabasePermission.DESCRIBE_TABLE);
        
        List<Map<String, Object>> result = dbConnectionService.executeDescribeTable(apiKey, tableName);
        return convertToCsv(result);
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

    private String convertToCsv(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "success,count,0\n";
        }

        StringBuilder csv = new StringBuilder();

        // Get all unique column names
        java.util.LinkedHashSet<String> columns = new java.util.LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columns.addAll(row.keySet());
        }

        // Write header
        csv.append(String.join(",", columns.stream()
                .map(this::escapeCsv)
                .toArray(String[]::new)))
           .append("\n");

        // Write data rows
        for (Map<String, Object> row : rows) {
            csv.append(columns.stream()
                    .map(col -> escapeCsv(row.get(col)))
                    .reduce((a, b) -> a + "," + b)
                    .orElse(""))
               .append("\n");
        }

        // Add metadata
        csv.append("success,count,").append(rows.size()).append("\n");

        return csv.toString();
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }

        String str = value.toString();
        
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"");
            return "\"" + str + "\"";
        }

        return str;
    }
}