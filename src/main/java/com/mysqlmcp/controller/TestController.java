package com.mysqlmcp.controller;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.entity.DatabaseConnection;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
public class TestController {

    private final DatabaseCredentialEncryptor encryptor;
    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;

    @PostMapping
    public String testConnection(@RequestBody TestRequest request){
        DatabaseConnection dc = new DatabaseConnection();
        dc.setHost(request.host);
        dc.setDatabaseName(request.databaseName);
        dc.setDbUsername(request.dbUsername);
        dc.setPort(request.port);
        dc.setEncryptedPassword(encryptor.encrypt(request.password));

        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.createJdbcTemplate(dc);

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "SUCCESS";
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        }
    }

    @Data
    public static class TestRequest{
        private String host;
        private Integer port;
        private String databaseName;
        private String dbUsername;
        private String password;
    }
}
