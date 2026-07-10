package com.mysqlmcp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TestConnectionRequest(
        @NotBlank String host,
        @NotNull Integer port,
        @NotBlank String databaseName,
        @NotBlank String dbUsername,
        @NotBlank String dbPassword
) {
}