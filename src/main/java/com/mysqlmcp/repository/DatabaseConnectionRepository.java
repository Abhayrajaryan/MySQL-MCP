package com.mysqlmcp.repository;

import com.mysqlmcp.entity.DatabaseConnection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {
}
