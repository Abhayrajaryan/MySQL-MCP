package com.mysqlmcp.controller;

import com.mysqlmcp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        Map<String, Object> result = authService.login(
                credentials.get("username"),
                credentials.get("password")
        );

        if (result.containsKey("code")) {
            String code = (String) result.get("code");
            int status = "INVALID_CREDENTIALS".equals(code) ? 401 : 400;
            return ResponseEntity.status(status).body(result);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/auth/logout")
    @ResponseBody
    public ResponseEntity<?> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}