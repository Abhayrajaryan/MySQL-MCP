package com.mysqlmcp.controller;

import com.mysqlmcp.config.SecurityDefaultsProperties;
import com.mysqlmcp.dto.response.ApiResponse;
import com.mysqlmcp.dto.response.SecurityDefaultsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the server-wide write/DDL kill-switch flags (read-only) so the
 * dashboard UI can reflect them — e.g. graying out permission checkboxes that
 * the backend would reject anyway — instead of letting an operator hit a
 * confusing failure after submitting the form.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/config")
public class SecurityDefaultsController {

    private final SecurityDefaultsProperties securityDefaults;

    @GetMapping("/security-defaults")
    public ResponseEntity<ApiResponse<SecurityDefaultsResponse>> getSecurityDefaults() {
        SecurityDefaultsResponse response = SecurityDefaultsResponse.builder()
                .writeOperationsEnabled(securityDefaults.isWriteOperationsEnabled())
                .ddlOperationsEnabled(securityDefaults.isDdlOperationsEnabled())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}