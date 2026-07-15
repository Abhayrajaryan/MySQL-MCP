package com.mysqlmcp.controller;

import com.mysqlmcp.dto.request.CreateApiKeyRequest;
import com.mysqlmcp.dto.response.ApiResponse;
import com.mysqlmcp.dto.response.CreateApiKeyResponse;
import com.mysqlmcp.service.DatabaseConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api-keys")
public class ApiKeyController {

    private final DatabaseConnectionService dbConnectionService;

    @PostMapping("/connections/{connectionId}")
    public ResponseEntity<ApiResponse<CreateApiKeyResponse>> createForConnection(
            @PathVariable Long connectionId,
            @RequestBody CreateApiKeyRequest request) {

        log.info("Creating new API key '{}' for connection id: {}", request.getName(), connectionId);
        String rawKey = dbConnectionService.generateApiKeyForConnection(connectionId, request.getName());

        CreateApiKeyResponse response = CreateApiKeyResponse.builder()
                .connectionId(connectionId)
                .apiKey(rawKey)
                .keyPrefix(rawKey.substring(0, Math.min(20, rawKey.length())) + "...")
                .name(request.getName())
                .build();

        ApiResponse<CreateApiKeyResponse> apiResponse = ApiResponse.success(
                response,
                "API key created successfully. Save it now — it will not be shown again."
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @GetMapping("/connections/{connectionId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getKeysWithPermissions(
            @PathVariable Long connectionId) {

        List<Map<String, Object>> keys = dbConnectionService.getApiKeysWithPermissions(connectionId);

        ApiResponse<List<Map<String, Object>>> apiResponse = ApiResponse.success(
                keys,
                "API keys retrieved"
        );

        return ResponseEntity.ok(apiResponse);
    }
}