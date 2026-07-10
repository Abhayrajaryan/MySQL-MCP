package com.mysqlmcp.controller;

import com.mysqlmcp.dto.request.CreateApiKeyRequest;
import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.enums.DatabasePermission;
import com.mysqlmcp.service.ApiKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyService.GeneratedApiKey generated = apiKeyService.createApiKey(
                request.databaseConnectionId(),
                request.name(),
                request.permissions(),
                request.expiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", generated.id(),
                "rawKey", generated.rawKey(),
                "prefix", generated.prefix(),
                "name", generated.name(),
                "isActive", generated.isActive(),
                "expiresAt", generated.expiresAt(),
                "message", "Store this API key securely. It will not be shown again."
        ));
    }

    @GetMapping
    public ResponseEntity<List<ApiKey>> getAll(@RequestParam Long databaseConnectionId) {
        List<ApiKey> keys = apiKeyService.findByDatabaseConnectionId(databaseConnectionId);
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiKey> getById(@PathVariable Long id) {
        ApiKey apiKey = apiKeyService.findById(id);
        return ResponseEntity.ok(apiKey);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiKey> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        ApiKey apiKey = apiKeyService.toggleStatus(id, active);
        return ResponseEntity.ok(apiKey);
    }
}