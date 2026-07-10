package com.mysqlmcp.controller;

import com.mysqlmcp.dto.request.CreateDatabaseConnectionRequest;
import com.mysqlmcp.dto.request.TestConnectionRequest;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.service.DatabaseConnectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/database-connections")
public class DatabaseConnectionController {

    private final DatabaseConnectionService databaseConnectionService;

    public DatabaseConnectionController(DatabaseConnectionService databaseConnectionService) {
        this.databaseConnectionService = databaseConnectionService;
    }

    @PostMapping
    public ResponseEntity<DatabaseConnection> create(@Valid @RequestBody CreateDatabaseConnectionRequest request) {
        // TODO: Get user from SecurityContext when auth is implemented
        DatabaseConnection connection = databaseConnectionService.create(null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(connection);
    }

    @GetMapping
    public ResponseEntity<List<DatabaseConnection>> getAll() {
        // TODO: Get user from SecurityContext
        List<DatabaseConnection> connections = databaseConnectionService.findByUserId(null);
        return ResponseEntity.ok(connections);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DatabaseConnection> getById(@PathVariable Long id) {
        // TODO: Get user from SecurityContext
        DatabaseConnection connection = databaseConnectionService.findByIdAndUserId(id, null);
        return ResponseEntity.ok(connection);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DatabaseConnection> update(@PathVariable Long id,
                                                     @Valid @RequestBody CreateDatabaseConnectionRequest request) {
        // TODO: Get user from SecurityContext
        DatabaseConnection connection = databaseConnectionService.update(id, null, request);
        return ResponseEntity.ok(connection);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // TODO: Get user from SecurityContext
        databaseConnectionService.delete(id, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Boolean>> testConnection(@Valid @RequestBody TestConnectionRequest request) {
        boolean success = databaseConnectionService.testConnection(request);
        return ResponseEntity.ok(Map.of("success", success));
    }
}