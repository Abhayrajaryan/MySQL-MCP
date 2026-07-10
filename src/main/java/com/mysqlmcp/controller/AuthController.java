package com.mysqlmcp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        // TODO: Implement user registration
        return ResponseEntity.ok(Map.of("message", "Registration not yet implemented"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        // TODO: Implement user login with JWT
        return ResponseEntity.ok(Map.of("message", "Login not yet implemented"));
    }
}