package com.aidev.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * 执行实例唯一标识符（值对象）。
 *
 * <p>不可变，基于UUID生成。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class ExecutionId {

    private final String value;

    private ExecutionId(String value) {
        this.value = Objects.requireNonNull(value, "ExecutionId value cannot be null");
    }

    /**
     * 生成新的执行ID。
     *
     * @return 新的 ExecutionId 实例
     */
    public static ExecutionId generate() {
        return new ExecutionId(UUID.randomUUID().toString());
    }

    /**
     * 从字符串创建 ExecutionId。
     *
     * @param value UUID 字符串
     * @return ExecutionId 实例
     * @throws IllegalArgumentException 如果格式无效
     */
    public static ExecutionId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ExecutionId cannot be null or blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ExecutionId format: " + value, e);
        }
        return new ExecutionId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionId)) return false;
        ExecutionId that = (ExecutionId) o;
        return Objects.equals(value, that.value);
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
