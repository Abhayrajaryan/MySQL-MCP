package com.mysqlmcp.repository;

import com.mysqlmcp.entity.DatabaseConnection;
import com.mysqlmcp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {
    List<DatabaseConnection> findByUser(User user);
    List<DatabaseConnection> findByUserId(Long userId);
    Optional<DatabaseConnection> findByIdAndUserId(Long id, Long userId);
}