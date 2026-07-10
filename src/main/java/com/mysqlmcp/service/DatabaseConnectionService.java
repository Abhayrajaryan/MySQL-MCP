package com.mysqlmcp.service;

import com.mysqlmcp.database.DatabaseCredentialEncryptor;
import com.mysqlmcp.database.DynamicJdbcTemplateProvider;
import com.mysqlmcp.database.JdbcUrlBuilder;
import com.mysqlmcp.dto.request.CreateDatabaseConnectionRequest;
import com.mysqlmcp.dto.request.TestConnectionRequest;
import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.entity.User;
import com.mysqlmcp.exception.DatabaseConnectionException;
import com.mysqlmcp.repository.DatabaseConnectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseConnectionService {

    private final DatabaseConnectionRepository repository;
    private final DatabaseCredentialEncryptor encryptor;
    private final DynamicJdbcTemplateProvider jdbcTemplateProvider;

    public DatabaseConnectionService(DatabaseConnectionRepository repository,
                                     DatabaseCredentialEncryptor encryptor,
                                     DynamicJdbcTemplateProvider jdbcTemplateProvider) {
        this.repository = repository;
        this.encryptor = encryptor;
        this.jdbcTemplateProvider = jdbcTemplateProvider;
    }

    public DatabaseConnection create(User user, CreateDatabaseConnectionRequest request) {
        DatabaseConnection connection = new DatabaseConnection();
        connection.setUser(user);
        connection.setName(request.name());
        connection.setHost(request.host());
        connection.setPort(request.port());
        connection.setDatabaseName(request.databaseName());
        connection.setDbUsername(request.dbUsername());
        connection.setEncryptedPassword(encryptor.encrypt(request.dbPassword()));
        connection.setIsActive(true);
        return repository.save(connection);
    }

    public List<DatabaseConnection> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    public DatabaseConnection findByIdAndUserId(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DatabaseConnectionException("Database connection not found"));
    }

    public DatabaseConnection update(Long id, Long userId, CreateDatabaseConnectionRequest request) {
        DatabaseConnection connection = findByIdAndUserId(id, userId);
        connection.setName(request.name());
        connection.setHost(request.host());
        connection.setPort(request.port());
        connection.setDatabaseName(request.databaseName());
        connection.setDbUsername(request.dbUsername());
        connection.setEncryptedPassword(encryptor.encrypt(request.dbPassword()));
        return repository.save(connection);
    }

    public void delete(Long id, Long userId) {
        DatabaseConnection connection = findByIdAndUserId(id, userId);
        repository.delete(connection);
    }

    public boolean testConnection(TestConnectionRequest request) {
        return jdbcTemplateProvider.testConnection(
                request.host(),
                request.port(),
                request.databaseName(),
                request.dbUsername(),
                request.dbPassword()
        );
    }
}