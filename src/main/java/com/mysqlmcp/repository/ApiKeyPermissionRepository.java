package com.mysqlmcp.repository;

import com.mysqlmcp.entity.ApiKeyPermission;
import com.mysqlmcp.enums.DatabasePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ApiKeyPermissionRepository extends JpaRepository<ApiKeyPermission, Long> {
    List<ApiKeyPermission> findByApiKeyId(Long apiKeyId);
    Set<DatabasePermission> findPermissionByApiKeyId(Long apiKeyId);
    boolean existsByApiKeyIdAndPermission(Long apiKeyId, DatabasePermission permission);
}