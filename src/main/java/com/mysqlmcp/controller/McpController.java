package com.mysqlmcp.controller;

import com.mysqlmcp.dto.request.McpQueryRequest;
import com.mysqlmcp.service.McpExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mcp")
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
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", "Query execution failed: " + e.getMessage()
            ));
        }
    }
}