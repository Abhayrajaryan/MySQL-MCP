package com.mysqlmcp.controller;

import com.mysqlmcp.dto.request.McpQueryRequest;
import com.mysqlmcp.service.McpExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpExecutionService mcpExecutionService;

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody McpQueryRequest request) {
        try {
            Object result = mcpExecutionService.execute(request.getApiKey(), request.getQuery());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));
        } catch (IllegalArgumentException e) {
            log.warn("MCP execution failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("MCP execution error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Query execution failed: " + e.getMessage()
            ));
        }
    }
}