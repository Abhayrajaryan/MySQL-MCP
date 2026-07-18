package com.mysqlmcp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecurityDefaultsResponse {
    private boolean writeOperationsEnabled;
    private boolean ddlOperationsEnabled;
}