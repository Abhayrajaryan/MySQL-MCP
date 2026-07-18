package com.mysqlmcp.controller;

import com.mysqlmcp.entity.ApiKey;
import com.mysqlmcp.repository.ApiKeyRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
public class McpOAuthController {

    private final ApiKeyRepository apiKeyRepo;
    
    // In-memory store for authorization codes (code -> {apiKey, redirectUri, codeChallenge})
    private final Map<String, AuthCode> authCodes = new ConcurrentHashMap<>();
    
    // In-memory store for access tokens (token -> apiKey)
    private final Map<String, String> accessTokens = new ConcurrentHashMap<>();

    @GetMapping("/authorize")
    public void authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("response_type") String responseType,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("code_challenge_method") String codeChallengeMethod,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {

        log.info("OAuth authorize request - client_id: {}, redirect_uri: {}, state: {}", clientId, redirectUri, state);

        // Validate the API key (client_id is the API key)
        String keyHash = hashApiKey(clientId);
        
        ApiKey apiKey = apiKeyRepo.findAll().stream()
                .filter(k -> k.getKeyHash().equals(keyHash))
                .findFirst()
                .orElse(null);

        if (apiKey == null || !apiKey.getIsActive()) {
            log.warn("OAuth authorize failed - invalid API key: {}", clientId);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
            return;
        }

        // Generate authorization code
        String authCode = generateSecureToken(32);
        
        AuthCode codeData = new AuthCode();
        codeData.apiKeyHash = keyHash;
        codeData.redirectUri = redirectUri;
        codeData.codeChallenge = codeChallenge;
        codeData.codeChallengeMethod = codeChallengeMethod;
        codeData.createdAt = System.currentTimeMillis();
        
        authCodes.put(authCode, codeData);
        
        // Clean up old codes (older than 5 minutes)
        cleanupOldCodes();

        // Redirect back to VS Code with the authorization code
        String redirectUrl = redirectUri + "?code=" + authCode + "&state=" + state;
        log.info("OAuth authorize success - redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam("code") String code,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("client_id") String clientId,
            @RequestParam("code_verifier") String codeVerifier) {

        log.info("OAuth token request - client_id: {}, code: {}", clientId, code);

        // Validate authorization code
        AuthCode authCode = authCodes.get(code);
        if (authCode == null) {
            log.warn("OAuth token failed - invalid authorization code");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_grant",
                "error_description", "Invalid authorization code"
            ));
        }

        // Verify redirect URI
        if (!authCode.redirectUri.equals(redirectUri)) {
            log.warn("OAuth token failed - redirect URI mismatch");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_grant",
                "error_description", "Redirect URI mismatch"
            ));
        }

        // Verify PKCE code challenge
        String expectedChallenge = base64UrlEncode(sha256(codeVerifier));
        if (!authCode.codeChallenge.equals(expectedChallenge)) {
            log.warn("OAuth token failed - code verifier mismatch");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "invalid_grant",
                "error_description", "Code verifier mismatch"
            ));
        }

        // Return the actual API key (client_id) as the access token
        // No need to generate new tokens - the API key IS the token
        authCodes.remove(code);

        log.info("OAuth token success - returning API key as access token");
        return ResponseEntity.ok(Map.of(
            "access_token", clientId,
            "token_type", "Bearer",
            "expires_in", 3600
        ));
    }

    @GetMapping("/mcp")
    public void handleMcpEndpoint(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // VS Code's MCP client may send requests to /mcp as a discovery endpoint
        // Redirect to a meaningful response
        response.setContentType("application/json");
        response.getWriter().write("{\"endpoints\": {\"authorize\": \"/authorize\", \"token\": \"/token\", \"execute\": \"/mcp/execute\"}}");
    }

    private String hashApiKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    private String generateSecureToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private void cleanupOldCodes() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        authCodes.values().removeIf(code -> code.createdAt < fiveMinutesAgo);
    }

    private void cleanupOldTokens() {
        // Simple cleanup - keep only the last 100 tokens
        if (accessTokens.size() > 100) {
            // Just clear old ones - in production use proper expiration
            accessTokens.clear();
        }
    }

    private static class AuthCode {
        String apiKeyHash;
        String redirectUri;
        String codeChallenge;
        String codeChallengeMethod;
        long createdAt;
    }
}