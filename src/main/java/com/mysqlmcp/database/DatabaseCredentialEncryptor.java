package com.mysqlmcp.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class DatabaseCredentialEncryptor {

    private final SecretKeySpec secretKey;

    public DatabaseCredentialEncryptor(@Value("${app.encryption.key}") String encryptionKey) {
        byte[] keyBytes = encryptionKey.getBytes();
        if (keyBytes.length < 16) {
            throw new IllegalArgumentException("Encryption key must be at least 16 characters long");
        }
        byte[] adjustedKey = new byte[16];
        System.arraycopy(keyBytes, 0, adjustedKey, 0, Math.min(keyBytes.length, 16));
        this.secretKey = new SecretKeySpec(adjustedKey, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt database password", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt database password", e);
        }
    }
}