package com.mysqlmcp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Powers the Connection and Operation filter dropdowns on the audit log UI. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogFilterOptionsResponse {
    private List<Option> connections;
    private List<String> operations;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Option {
        private Long id;
        private String name;
    }
}