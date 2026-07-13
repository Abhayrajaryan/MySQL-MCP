package com.mysqlmcp.database;

import com.mysqlmcp.entity.DatabaseConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;


@Component
@RequiredArgsConstructor
public class DynamicJdbcTemplateProvider {

    private final DatabaseCredentialEncryptor credentialEncryptor;

    private String buildJdbcUrl(DatabaseConnection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("DatabaseConnection must not be null");
        }
        if (connection.getHost() == null || connection.getHost().isBlank()) {
            throw new IllegalArgumentException("Host must not be null or blank");
        }
        if (connection.getPort() == null || connection.getPort() < 1 || connection.getPort() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + connection.getPort());
        }
        if (connection.getDatabaseName() == null || connection.getDatabaseName().isBlank()) {
            throw new IllegalArgumentException("Database name must not be null or blank");
        }

        return "jdbc:mysql://" + connection.getHost() + ":" + connection.getPort()
                + "/" + connection.getDatabaseName();
    }

    /**
     * Creates a DriverManagerDataSource from a JDBC URL and credentials.
     */
    private DataSource createDataSource(String jdbcUrl, String username, String password) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("JDBC URL must not be null or blank");
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        return dataSource;
    }

    /**
     * Builds the full JDBC URL from a DatabaseConnection, decrypts the stored
     * password, creates a DataSource, and wraps it in a JdbcTemplate.
     */
    public JdbcTemplate createJdbcTemplate(DatabaseConnection connection) {
        String jdbcUrl = buildJdbcUrl(connection);
        String decryptedPassword = credentialEncryptor.decrypt(connection.getEncryptedPassword());
        DataSource dataSource = createDataSource(jdbcUrl, connection.getDbUsername(), decryptedPassword);
        return new JdbcTemplate(dataSource);
    }
}