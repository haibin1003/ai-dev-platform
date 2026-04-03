package com.aidev.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * 工作流唯一标识符（值对象）。
 *
 * <p>不可变，基于UUID生成。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class WorkflowId {

    private final String value;

    private WorkflowId(String value) {
        this.value = Objects.requireNonNull(value, "WorkflowId value cannot be null");
    }

    /**
     * 生成新的工作流ID。
     *
     * @return 新的 WorkflowId 实例
     */
    public static WorkflowId generate() {
        return new WorkflowId(UUID.randomUUID().toString());
    }

    /**
     * 从字符串创建 WorkflowId。
     *
     * @param value UUID 字符串
     * @return WorkflowId 实例
     * @throws IllegalArgumentException 如果格式无效
     */
    public static WorkflowId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WorkflowId cannot be null or blank");
        }
        // 验证 UUID 格式
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid WorkflowId format: " + value, e);
        }
        return new WorkflowId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkflowId)) return false;
        WorkflowId that = (WorkflowId) o;
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
