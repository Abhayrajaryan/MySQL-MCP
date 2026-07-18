package com.mysqlmcp.mcp;

import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.service.RemoteQueryExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseModificationTools {

    private final RemoteQueryExecutionService remoteQueryExecutionService;

    @Tool(description = "Execute an INSERT query. Requires INSERT permission. Returns affected rows count in CSV format.")
    public String executeInsert(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "INSERT query to execute") String query) {

        int affectedRows = remoteQueryExecutionService.executeWrite(apiKey, DatabasePermission.INSERT, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute an UPDATE query. Requires UPDATE permission. Returns affected rows count in CSV format.")
    public String executeUpdate(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "UPDATE query to execute") String query) {

        int affectedRows = remoteQueryExecutionService.executeWrite(apiKey, DatabasePermission.UPDATE, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute a DELETE query. Requires DELETE permission. Returns affected rows count in CSV format.")
    public String executeDelete(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "DELETE query to execute") String query) {

        int affectedRows = remoteQueryExecutionService.executeWrite(apiKey, DatabasePermission.DELETE, query);
        return "success,affectedRows," + affectedRows + "\n";
    }

    @Tool(description = "Execute a CREATE TABLE query. Requires CREATE_TABLE permission. Returns success message in CSV format.")
    public String createTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "CREATE TABLE query to execute") String query) {

        remoteQueryExecutionService.executeDdl(apiKey, DatabasePermission.CREATE_TABLE, query);
        return "success,message,Table created successfully\n";
    }

    @Tool(description = "Execute an ALTER TABLE query. Requires ALTER_TABLE permission. Returns success message in CSV format.")
    public String alterTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "ALTER TABLE query to execute") String query) {

        remoteQueryExecutionService.executeDdl(apiKey, DatabasePermission.ALTER_TABLE, query);
        return "success,message,Table altered successfully\n";
    }

    @Tool(description = "Execute a DROP TABLE query. Requires DROP_TABLE permission. Returns success message in CSV format.")
    public String dropTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "DROP TABLE query to execute") String query) {

        remoteQueryExecutionService.executeDdl(apiKey, DatabasePermission.DROP_TABLE, query);
        return "success,message,Table dropped successfully\n";
    }
}