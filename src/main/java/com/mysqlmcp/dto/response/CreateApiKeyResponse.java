package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreateApiKeyResponse {
    private Long connectionId;
    private String apiKey;  // full raw key returned only once
    private String keyPrefix;
    private String name;
}