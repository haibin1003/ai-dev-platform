package com.aidev.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * 租户ID（值对象）。
 *
 * <p>不可变，封装租户标识符的校验规则。
 * 支持 UUID 格式和自定义字符串格式的租户ID。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class TenantId {

    private final String value;

    private TenantId(String value) {
        this.value = Objects.requireNonNull(value, "TenantId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be blank");
        }
    }

    /**
     * 从字符串创建租户ID。
     *
     * @param value 租户ID字符串
     * @return TenantId
     * @throws IllegalArgumentException 如果值为空或格式无效
     */
    public static TenantId of(String value) {
        return new TenantId(value);
    }

    /**
     * 生成新的随机租户ID（UUID格式）。
     *
     * @return 新的 TenantId
     */
    public static TenantId generate() {
        return new TenantId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantId tenantId = (TenantId) o;
        return value.equals(tenantId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
