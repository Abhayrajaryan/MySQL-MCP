package com.mysqlmcp.service;

import lombok.extern.slf4j.Slf4j;
import com.mysqlmcp.entity.BlacklistedToken;
import com.mysqlmcp.repository.BlacklistedTokenRepository;
import com.mysqlmcp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepo;
    private final JwtUtil jwtUtil;

    @Transactional
    public void blacklist(String token) {
        String tokenHash = hashToken(token);

        if (blacklistedTokenRepo.existsByTokenHash(tokenHash)) {
            return;
        }

        LocalDateTime expiresAt = jwtUtil.extractExpiration(token)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        BlacklistedToken entry = new BlacklistedToken();
        entry.setTokenHash(tokenHash);
        entry.setExpiresAt(expiresAt);
        blacklistedTokenRepo.save(entry);
        log.info("Token blacklisted");
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokenRepo.existsByTokenHash(hashToken(token));
    }

    public boolean isExpired(BlacklistedToken entry) {
        return entry.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}