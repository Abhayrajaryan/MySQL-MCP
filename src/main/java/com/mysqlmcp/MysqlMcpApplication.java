package com.mysqlmcp;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class MysqlMcpApplication {

    @Value("${mysql-mcp.auth.username}")
    private String configuredUsername;

    @Value("${mysql-mcp.auth.password}")
    private String configuredPassword;

    public static void main(String[] args) {
        SpringApplication.run(MysqlMcpApplication.class, args);
    }

    @PostConstruct
    public void warnIfDefaultCredentials() {
        if ("admin".equals(configuredUsername) && "admin123".equals(configuredPassword)) {
            log.warn("*****************************************************************");
            log.warn("*                                                               *");
            log.warn("*  DEFAULT CREDENTIALS ARE ACTIVE!                              *");
            log.warn("*                                                               *");
            log.warn("*  Username: admin                                              *");
            log.warn("*  Password: admin123                                           *");
            log.warn("*                                                               *");
            log.warn("*  Override them using environment variables:                   *");
            log.warn("*    MYSQL_MCP_AUTH_USERNAME=yourname                           *");
            log.warn("*    MYSQL_MCP_AUTH_PASSWORD=your-strong-password               *");
            log.warn("*                                                               *");
            log.warn("*****************************************************************");
        }
    }
}