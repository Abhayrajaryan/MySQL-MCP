package com.mysqlmcp.repository;

import com.mysqlmcp.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    List<ApiKey> findByDatabaseConnectionId(Long databaseConnectionId);
    Optional<ApiKey> findByKeyHash(String keyHash);
    Optional<ApiKey> findByIdAndDatabaseConnectionId(Long id, Long databaseConnectionId);
}