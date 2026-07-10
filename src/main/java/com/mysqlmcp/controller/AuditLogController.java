package com.mysqlmcp.controller;

import com.mysqlmcp.entity.AuditLog;
import com.mysqlmcp.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAll() {
        return ResponseEntity.ok(auditLogService.findAll());
    }

    @GetMapping("/by-api-key/{apiKeyId}")
    public ResponseEntity<List<AuditLog>> getByApiKeyId(@PathVariable Long apiKeyId) {
        return ResponseEntity.ok(auditLogService.findByApiKeyId(apiKeyId));
    }

    @GetMapping("/by-database-connection/{databaseConnectionId}")
    public ResponseEntity<List<AuditLog>> getByDatabaseConnectionId(@PathVariable Long databaseConnectionId) {
        return ResponseEntity.ok(auditLogService.findByDatabaseConnectionId(databaseConnectionId));
    }
}