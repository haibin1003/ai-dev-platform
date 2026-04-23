package com.aidev.infrastructure.security;

import com.aidev.domain.model.valueobject.EncryptedValue;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

/**
 * AES-256-GCM 加密服务。
 *
 * <p>提供敏感数据（如 API Key）的加密、解密和密钥轮换功能。
 * 使用 AES-256-GCM 模式，每次加密生成随机 IV，确保相同明文产生不同密文。
 *
 * <p>密钥通过环境变量 {@code PLATFORM_ENCRYPTION_KEY} 配置，必须为 32 字节 Base64 编码。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Service
public class AesEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(AesEncryptionService.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 32; // 256 bits
    private static final int IV_SIZE = 12;  // 96 bits for GCM
    private static final int TAG_SIZE = 128; // 128 bits authentication tag

    @Value("${platform.encryption.key:}")
    private String encryptionKeyBase64;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    @PostConstruct
    public void init() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            logger.warn("PLATFORM_ENCRYPTION_KEY not set, generating a random key for development. " +
                        "DO NOT USE IN PRODUCTION!");
            byte[] randomKey = new byte[KEY_SIZE];
            secureRandom.nextBytes(randomKey);
            this.secretKey = new SecretKeySpec(randomKey, ALGORITHM);
            logger.info("Generated development encryption key: {}",
                        Base64.getEncoder().encodeToString(randomKey));
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
            if (keyBytes.length != KEY_SIZE) {
                throw new IllegalStateException(
                    "PLATFORM_ENCRYPTION_KEY must be " + KEY_SIZE + " bytes after Base64 decode, got " + keyBytes.length
                );
            }
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            logger.info("Encryption key loaded successfully ({} bytes)", keyBytes.length);
        }
    }

    /**
     * 加密明文。
     *
     * @param plaintext 明文
     * @return 加密后的值对象
     * @throws EncryptionException 如果加密失败
     */
    public EncryptedValue encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // GCM mode: encrypted contains ciphertext + auth tag
            // Split: ciphertext = all except last 16 bytes, tag = last 16 bytes
            int tagLength = TAG_SIZE / 8;
            int ciphertextLength = encrypted.length - tagLength;

            byte[] ciphertext = new byte[ciphertextLength];
            byte[] tag = new byte[tagLength];
            System.arraycopy(encrypted, 0, ciphertext, 0, ciphertextLength);
            System.arraycopy(encrypted, ciphertextLength, tag, 0, tagLength);

            return EncryptedValue.of(ciphertext, iv, tag);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * 解密密文。
     *
     * @param encryptedValue 加密值
     * @return 明文
     * @throws EncryptionException 如果解密失败（密钥错误、数据篡改等）
     */
    public String decrypt(EncryptedValue encryptedValue) {
        if (encryptedValue == null) {
            throw new IllegalArgumentException("EncryptedValue cannot be null");
        }
        try {
            byte[] iv = encryptedValue.getIv();
            byte[] ciphertext = encryptedValue.getCiphertext();
            byte[] tag = encryptedValue.getTag();

            // Reconstruct GCM output: ciphertext + tag
            byte[] encrypted = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, encrypted, 0, ciphertext.length);
            System.arraycopy(tag, 0, encrypted, ciphertext.length, tag.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException("Failed to decrypt data (possible key mismatch or tampering)", e);
        }
    }

    /**
     * 使用新密钥重新加密一批数据。
     *
     * @param encryptedValues 待轮换的加密值列表
     * @param newKeyBase64 新密钥（32字节 Base64 编码）
     * @return 轮换后的加密值列表
     */
    public List<EncryptedValue> rotateKeys(List<EncryptedValue> encryptedValues, String newKeyBase64) {
        byte[] newKeyBytes = Base64.getDecoder().decode(newKeyBase64);
        if (newKeyBytes.length != KEY_SIZE) {
            throw new IllegalArgumentException("New key must be " + KEY_SIZE + " bytes, got " + newKeyBytes.length);
        }

        SecretKey newKey = new SecretKeySpec(newKeyBytes, ALGORITHM);
        SecretKey oldKey = this.secretKey;

        try {
            // Temporarily switch to old key for decryption
            this.secretKey = oldKey;

            List<EncryptedValue> rotated = encryptedValues.stream()
                .map(ev -> {
                    String plaintext = decrypt(ev);
                    // Switch to new key for encryption
                    this.secretKey = newKey;
                    EncryptedValue reEncrypted = encrypt(plaintext);
                    // Switch back to old key for next decryption
                    this.secretKey = oldKey;
                    return reEncrypted;
                })
                .toList();

            // Finally adopt new key
            this.secretKey = newKey;
            return rotated;
        } finally {
            this.secretKey = newKey;
        }
    }

    /**
     * 加密异常。
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
