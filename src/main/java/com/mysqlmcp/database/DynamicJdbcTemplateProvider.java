package com.mysqlmcp.database;

import com.mysqlmcp.config.MysqlMcpProperties;
import com.mysqlmcp.exception.DatabaseConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

@Component
public class DynamicJdbcTemplateProvider {

    private final JdbcUrlBuilder jdbcUrlBuilder;
    private final MysqlMcpProperties properties;

    public DynamicJdbcTemplateProvider(JdbcUrlBuilder jdbcUrlBuilder, MysqlMcpProperties properties) {
        this.jdbcUrlBuilder = jdbcUrlBuilder;
        this.properties = properties;
    }

    public JdbcTemplate createJdbcTemplate(String host, Integer port, String databaseName,
                                           String username, String password) {
        String url = jdbcUrlBuilder.buildUrl(host, port, databaseName);
        try {
            DataSource dataSource = createDataSource(url, username, password);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.setQueryTimeout(properties.getExecution().getQueryTimeoutSeconds());
            return jdbcTemplate;
        } catch (Exception e) {
            throw new DatabaseConnectionException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    public boolean testConnection(String host, Integer port, String databaseName,
                                  String username, String password) {
        String url = jdbcUrlBuilder.buildUrl(host, port, databaseName);
        Properties connectionProps = new Properties();
        connectionProps.setProperty("user", username);
        connectionProps.setProperty("password", password);
        connectionProps.setProperty("connectTimeout", String.valueOf(properties.getConnection().getTimeoutSeconds() * 1000));

        try (Connection connection = DriverManager.getConnection(url, connectionProps)) {
            return connection.isValid(properties.getConnection().getTimeoutSeconds());
        } catch (Exception e) {
            throw new DatabaseConnectionException("Database connection test failed: " + e.getMessage(), e);
        }
    }

    private DataSource createDataSource(String url, String username, String password) {
        return new org.springframework.jdbc.datasource.DriverManagerDataSource(url, username, password);
    }
}