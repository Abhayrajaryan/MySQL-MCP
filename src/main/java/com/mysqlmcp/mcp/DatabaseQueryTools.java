package com.mysqlmcp.mcp;

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
public class DatabaseQueryTools {

    private final RemoteQueryExecutionService remoteQueryExecutionService;
    private final CsvUtils csvUtils;

    @Tool(description = "Execute a SELECT query on the database. Requires SELECT permission. Returns results in CSV format.")
    public String executeSelect(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "SELECT query to execute") String query) {

        List<Map<String, Object>> result = remoteQueryExecutionService.executeSelect(apiKey, query);
        return csvUtils.convertToCsv(result);
    }

    @Tool(description = "Execute an EXPLAIN query to analyze query performance. Requires EXPLAIN permission. Returns results in CSV format.")
    public String explainQuery(
            @ToolParam(description = "API key for authentication") String apiKey,
            @ToolParam(description = "Query to explain") String query) {

        List<Map<String, Object>> result = remoteQueryExecutionService.executeExplain(apiKey, query);
        return csvUtils.convertToCsv(result);
    }
}