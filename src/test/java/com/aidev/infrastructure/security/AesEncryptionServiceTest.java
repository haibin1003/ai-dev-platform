package com.aidev.infrastructure.security;

import com.aidev.domain.model.valueobject.EncryptedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesEncryptionService 单元测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
class AesEncryptionServiceTest {

    private AesEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new AesEncryptionService();
        encryptionService.init();
    }

    @Test
    void shouldEncryptPlaintext() {
        String plaintext = "sk-test-api-key-12345";

        EncryptedValue encrypted = encryptionService.encrypt(plaintext);

        assertNotNull(encrypted);
        assertTrue(encrypted.getCiphertext().length > 0);
        assertTrue(encrypted.getIv().length > 0);
        assertTrue(encrypted.getTag().length > 0);
    }

    @Test
    void shouldProduceDifferentCiphertextForSamePlaintext() {
        String plaintext = "same-text";

        EncryptedValue encrypted1 = encryptionService.encrypt(plaintext);
        EncryptedValue encrypted2 = encryptionService.encrypt(plaintext);

        assertNotEquals(
            Base64.getEncoder().encodeToString(encrypted1.getCiphertext()),
            Base64.getEncoder().encodeToString(encrypted2.getCiphertext())
        );
    }

    @Test
    void shouldDecryptToOriginalPlaintext() {
        String plaintext = "sk-test-api-key-12345";

        EncryptedValue encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldDecryptStoredString() {
        String plaintext = "my-secret-api-key";
        EncryptedValue encrypted = encryptionService.encrypt(plaintext);
        String stored = encrypted.toStoredString();

        EncryptedValue parsed = EncryptedValue.fromStoredString(stored);
        String decrypted = encryptionService.decrypt(parsed);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldThrowOnDecryptWithWrongKey() {
        String plaintext = "secret";
        EncryptedValue encrypted = encryptionService.encrypt(plaintext);

        // Create another service with different key
        AesEncryptionService otherService = new AesEncryptionService();
        otherService.init();

        assertThrows(AesEncryptionService.EncryptionException.class,
            () -> otherService.decrypt(encrypted));
    }

    @Test
    void shouldRejectNullPlaintext() {
        assertThrows(IllegalArgumentException.class, () -> encryptionService.encrypt(null));
    }

    @Test
    void shouldRejectNullEncryptedValue() {
        assertThrows(IllegalArgumentException.class, () -> encryptionService.decrypt(null));
    }

    @Test
    void shouldRotateKeys() {
        String plaintext1 = "key-1";
        String plaintext2 = "key-2";
        EncryptedValue encrypted1 = encryptionService.encrypt(plaintext1);
        EncryptedValue encrypted2 = encryptionService.encrypt(plaintext2);

        byte[] newKey = new byte[32];
        java.security.SecureRandom random = new java.security.SecureRandom();
        random.nextBytes(newKey);
        String newKeyBase64 = Base64.getEncoder().encodeToString(newKey);

        List<EncryptedValue> rotated = encryptionService.rotateKeys(
            List.of(encrypted1, encrypted2), newKeyBase64);

        assertEquals(2, rotated.size());
        assertEquals(plaintext1, encryptionService.decrypt(rotated.get(0)));
        assertEquals(plaintext2, encryptionService.decrypt(rotated.get(1)));
    }

    @Test
    void shouldEncryptChineseText() {
        String plaintext = "中文API密钥测试";

        EncryptedValue encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }
}
