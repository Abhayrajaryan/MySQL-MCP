package com.mysqlmcp.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateApiKeyRequest {
    private String name;
    private List<String> permissions;
}