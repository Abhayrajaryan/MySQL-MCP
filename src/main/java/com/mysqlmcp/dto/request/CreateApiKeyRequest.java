package com.mysqlmcp.dto.request;

import com.mysqlmcp.enums.DatabasePermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Set;

public record CreateApiKeyRequest(
        @NotNull Long databaseConnectionId,
        @NotBlank String name,
        @NotNull Set<DatabasePermission> permissions,
        LocalDateTime expiresAt
) {
}