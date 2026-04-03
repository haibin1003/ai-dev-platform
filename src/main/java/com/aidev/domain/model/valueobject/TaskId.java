package com.aidev.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

/**
 * 任务唯一标识符（值对象）。
 *
 * <p>不可变，基于UUID生成。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class TaskId {

    private final String value;

    private TaskId(String value) {
        this.value = Objects.requireNonNull(value, "TaskId value cannot be null");
    }

    /**
     * 生成新的任务ID。
     *
     * @return 新的 TaskId 实例
     */
    public static TaskId generate() {
        return new TaskId(UUID.randomUUID().toString());
    }

    /**
     * 从字符串创建 TaskId。
     *
     * @param value UUID 字符串
     * @return TaskId 实例
     * @throws IllegalArgumentException 如果格式无效
     */
    public static TaskId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskId cannot be null or blank");
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TaskId format: " + value, e);
        }
        return new TaskId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskId)) return false;
        TaskId that = (TaskId) o;
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
