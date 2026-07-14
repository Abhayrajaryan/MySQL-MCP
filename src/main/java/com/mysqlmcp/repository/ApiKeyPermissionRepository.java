package com.mysqlmcp.repository;

import com.mysqlmcp.entity.ApiKeyPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyPermissionRepository extends JpaRepository<ApiKeyPermission, Long> {
}