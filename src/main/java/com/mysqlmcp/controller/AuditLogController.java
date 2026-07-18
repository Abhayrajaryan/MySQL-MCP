package com.mysqlmcp.controller;

import com.mysqlmcp.dto.response.ApiResponse;
import com.mysqlmcp.dto.response.AuditLogFilterOptionsResponse;
import com.mysqlmcp.dto.response.AuditLogResponse;
import com.mysqlmcp.dto.response.AuditLogSummaryResponse;
import com.mysqlmcp.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Read-only API for the audit trail: which API key requested what and how
 * long it took. Protected by the same JWT-based session auth as the rest of
 * {@code /api/**} (see SecurityConfig) — only the dashboard operator can see
 * it, not the MCP API keys themselves.
 *
 * <p>Filtering is deliberately limited to Connection, Operation, From and To —
 * everything else is just displayed per-row, not filterable.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> search(
            @RequestParam(required = false) Long connectionId,
            @RequestParam(required = false) String operation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "id"));
        Page<AuditLogResponse> result = auditLogQueryService.search(connectionId, operation, from, to, pageable);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AuditLogSummaryResponse>> summary() {
        return ResponseEntity.ok(ApiResponse.success(auditLogQueryService.summary()));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<AuditLogFilterOptionsResponse>> filterOptions() {
        return ResponseEntity.ok(ApiResponse.success(auditLogQueryService.filterOptions()));
    }
}