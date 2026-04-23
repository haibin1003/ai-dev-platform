package com.aidev.domain.model.valueobject;

import java.util.Base64;
import java.util.Objects;

/**
 * 加密值（值对象）。
 *
 * <p>封装 AES-256-GCM 加密后的数据，包含密文、初始化向量（IV）和认证标签（Tag）。
 * 存储格式：{@code base64(ciphertext):base64(iv):base64(tag)}
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class EncryptedValue {

    private final byte[] ciphertext;
    private final byte[] iv;
    private final byte[] tag;

    private EncryptedValue(byte[] ciphertext, byte[] iv, byte[] tag) {
        this.ciphertext = Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        this.iv = Objects.requireNonNull(iv, "iv cannot be null");
        this.tag = Objects.requireNonNull(tag, "tag cannot be null");
    }

    /**
     * 从原始组件创建加密值。
     *
     * @param ciphertext 密文
     * @param iv 初始化向量
     * @param tag 认证标签
     * @return EncryptedValue
     */
    public static EncryptedValue of(byte[] ciphertext, byte[] iv, byte[] tag) {
        return new EncryptedValue(ciphertext.clone(), iv.clone(), tag.clone());
    }

    /**
     * 从存储字符串解析加密值。
     * <p>格式：{@code base64(ciphertext):base64(iv):base64(tag)}
     *
     * @param storedValue 存储字符串
     * @return EncryptedValue
     * @throws IllegalArgumentException 如果格式无效
     */
    public static EncryptedValue fromStoredString(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            throw new IllegalArgumentException("Stored value cannot be null or blank");
        }
        String[] parts = storedValue.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted value format, expected 3 parts separated by ':'");
        }
        Base64.Decoder decoder = Base64.getDecoder();
        return new EncryptedValue(
            decoder.decode(parts[0]),
            decoder.decode(parts[1]),
            decoder.decode(parts[2])
        );
    }

    /**
     * 转换为存储字符串。
     * <p>格式：{@code base64(ciphertext):base64(iv):base64(tag)}
     *
     * @return 存储字符串
     */
    public String toStoredString() {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(ciphertext) + ":" +
               encoder.encodeToString(iv) + ":" +
               encoder.encodeToString(tag);
    }

    public byte[] getCiphertext() {
        return ciphertext.clone();
    }

    public byte[] getIv() {
        return iv.clone();
    }

    public byte[] getTag() {
        return tag.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedValue that = (EncryptedValue) o;
        return java.util.Arrays.equals(ciphertext, that.ciphertext) &&
               java.util.Arrays.equals(iv, that.iv) &&
               java.util.Arrays.equals(tag, that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            java.util.Arrays.hashCode(ciphertext),
            java.util.Arrays.hashCode(iv),
            java.util.Arrays.hashCode(tag)
        );
    }

    @Override
    public String toString() {
        return "EncryptedValue{length=" + ciphertext.length + ", ivLength=" + iv.length + ", tagLength=" + tag.length + "}";
    }
}
