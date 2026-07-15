package com.mysqlmcp.controller;

import com.mysqlmcp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "dashboard";
    }

    @PostMapping("/auth/login")
    @ResponseBody
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        log.info("Login attempt for user: {}", username);

        Map<String, Object> result = authService.login(username, credentials.get("password"));

        if (result.containsKey("code")) {
            String code = (String) result.get("code");
            int status = "INVALID_CREDENTIALS".equals(code) ? 401 : 400;
            log.warn("Login failed for user: {} - {}", username, code);
            return ResponseEntity.status(status).body(result);
        }

        log.info("Login successful for user: {}", username);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/auth/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpServletRequest request) {
        log.info("Logout request received");
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}