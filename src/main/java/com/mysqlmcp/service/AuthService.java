package com.mysqlmcp.service;

import com.mysqlmcp.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${mysql-mcp.auth.username}")
    private String configuredUsername;

    @Value("${mysql-mcp.auth.password}")
    private String configuredPassword;

    public Map<String, Object> login(String username, String password) {
        if (username == null || password == null) {
            log.warn("Login attempt with null credentials");
            return Map.of(
                    "code", "INVALID_INPUT",
                    "message", "Username and password are required"
            );
        }

        if (!configuredUsername.equals(username) || !configuredPassword.equals(password)) {
            log.warn("Login failed for user: {} - invalid credentials", username);
            return Map.of(
                    "code", "INVALID_CREDENTIALS",
                    "message", "Invalid username or password"
            );
        }

        log.info("Login successful for user: {}", username);
        String accessToken = jwtUtil.generateAccessToken(username);

        return Map.of(
                "accessToken", accessToken,
                "tokenType", "Bearer",
                "expiresIn", 6 * 60 * 60
        );
    }

    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Logging out user, blacklisting token");
            tokenBlacklistService.blacklist(token);
        } else {
            log.warn("Logout request without Bearer token");
        }
    }
}