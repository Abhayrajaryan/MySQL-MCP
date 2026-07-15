package com.mysqlmcp.controller;

import com.mysqlmcp.dto.request.UpsertDatabaseConnectionRequest;
import com.mysqlmcp.dto.response.ApiResponse;
import com.mysqlmcp.dto.response.ConnectionDetailResponse;
import com.mysqlmcp.dto.response.ConnectionListItem;
import com.mysqlmcp.dto.response.UpsertConnectionResponse;
import com.mysqlmcp.service.DatabaseConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/database-connections")
public class DatabaseConnectionController {

    private final DatabaseConnectionService dbConnectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<UpsertConnectionResponse>> upsert(
            @RequestBody UpsertDatabaseConnectionRequest request) {

        log.info("Upsert database connection: {}", request.getName());
        UpsertConnectionResponse response = dbConnectionService.upsert(request);
        HttpStatus status = response.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;

        ApiResponse<UpsertConnectionResponse> apiResponse = ApiResponse.success(
                response,
                response.isCreated() ? "Database connection created" : "Database connection updated"
        );

        return ResponseEntity.status(status).body(apiResponse);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConnectionListItem>>> getAll() {
        log.debug("Fetching all database connections");
        List<ConnectionListItem> connections = dbConnectionService.getAll();

        ApiResponse<List<ConnectionListItem>> apiResponse = ApiResponse.success(
                connections,
                "Found " + connections.size() + " connection(s)"
        );

        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConnectionDetailResponse>> findById(@PathVariable Long id) {
        log.debug("Fetching database connection with id: {}", id);
        ConnectionDetailResponse response = dbConnectionService.findById(id);

        ApiResponse<ConnectionDetailResponse> apiResponse = ApiResponse.success(
                response,
                "Connection details retrieved"
        );

        return ResponseEntity.ok(apiResponse);
    }
}