package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogFilterOptionsResponse {
    private List<Option> connections;
    private List<ApiKeyOption> apiKeys;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Option {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ApiKeyOption {
        private Long id;
        private String name;
        private Long connectionId;
    }
}