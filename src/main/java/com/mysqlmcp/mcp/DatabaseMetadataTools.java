package com.mysqlmcp.mcp;

import com.mysqlmcp.repository.ApiKeyPermissionRepository;
import com.mysqlmcp.repository.ApiKeyRepository;
import com.mysqlmcp.service.ApiKeyAuthService;
import com.mysqlmcp.service.RemoteQueryExecutionService;
import com.mysqlmcp.util.CsvUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatabaseMetadataTools {

    private final RemoteQueryExecutionService remoteQueryExecutionService;
    private final ApiKeyAuthService apiKeyAuthService;
    private final CsvUtils csvUtils;

    @Tool(description = "Show all tables in the database. Requires SHOW_TABLES permission.")
    public String showTables(
            @ToolParam(description = "API key for authentication") String apiKey) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.SHOW_TABLES);

        // This will be implemented to return CSV format
        List<Map<String, Object>> result = remoteQueryExecutionService.executeShowTables(apiKey);
        return csvUtils.convertToCsv(result);
    }

    @Tool(description = "Describe the structure of a table. Requires DESCRIBE_TABLE permission.")
    public String describeTable(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "Name of the table to describe") String tableName) {

        apiKeyAuthService.validateApiKeyAndPermission(apiKey, com.mysqlmcp.enums.DatabasePermission.DESCRIBE_TABLE);

        List<Map<String, Object>> result = remoteQueryExecutionService.executeDescribeTable(apiKey, tableName);
        return csvUtils.convertToCsv(result);
    }
}