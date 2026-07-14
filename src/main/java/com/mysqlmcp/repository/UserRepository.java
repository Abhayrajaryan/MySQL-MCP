package com.mysqlmcp.repository;

import com.mysqlmcp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}