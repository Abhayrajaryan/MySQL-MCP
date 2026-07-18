package com.mysqlmcp.mcp;

import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.service.ApiKeyAuthService;
import com.mysqlmcp.service.RemoteQueryExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseModificationTools {

    private final RemoteQueryExecutionService remoteQueryExecutionService;
    private final ApiKeyAuthService apiKeyAuthService;

    @Tool(description = "Execute an INSERT query. Requires INSERT permission. Returns affected rows count in CSV format.")
    public String executeInsert(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "INSERT query to execute") String query) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.INSERT);

        int affectedRows = remoteQueryExecutionService.executeUpdate(apiKey, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute an UPDATE query. Requires UPDATE permission. Returns affected rows count in CSV format.")
    public String executeUpdate(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "UPDATE query to execute") String query) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.UPDATE);

        int affectedRows = remoteQueryExecutionService.executeUpdate(apiKey, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute a DELETE query. Requires DELETE permission. Returns affected rows count in CSV format.")
    public String executeDelete(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "DELETE query to execute") String query) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.DELETE);

        int affectedRows = remoteQueryExecutionService.executeUpdate(apiKey, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute a CREATE TABLE query. Requires CREATE_TABLE permission. Returns success message in CSV format.")
    public String createTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "CREATE TABLE query to execute") String query) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.CREATE_TABLE);

        remoteQueryExecutionService.executeDdl(apiKey, query);
        return "success,message,Table created successfully\n";
    }

    @Tool(description = "Execute an ALTER TABLE query. Requires ALTER_TABLE permission. Returns success message in CSV format.")
    public String alterTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "ALTER TABLE query to execute") String query) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.ALTER_TABLE);

        remoteQueryExecutionService.executeDdl(apiKey, query);
        return "success,message,Table altered successfully\n";
    }

    @Tool(description = "Execute a DROP TABLE query. Requires DROP_TABLE permission. Returns success message in CSV format.")
    public String dropTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "DROP TABLE query to execute") String query) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.DROP_TABLE);

        remoteQueryExecutionService.executeDdl(apiKey, query);
        return "success,message,Table dropped successfully\n";
    }
}