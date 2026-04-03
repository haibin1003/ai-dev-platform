package com.aidev.domain.model.valueobject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 任务状态（值对象）。
 *
 * <p>不可变枚举，定义完整的状态转换规则。
 *
 * @author AI Assistant
 * @since 1.0
 */
public enum TaskStatus {
    /**
     * 待处理：依赖未满足
     */
    PENDING,

    /**
     * 就绪：依赖已满足，等待调度
     */
    READY,

    /**
     * 运行中：正在执行
     */
    RUNNING,

    /**
     * 已完成：执行成功
     */
    COMPLETED,

    /**
     * 已失败：执行失败
     */
    FAILED,

    /**
     * 已取消：手动取消或超时
     */
    CANCELLED;

    // 状态转换规则定义
    private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS;

    static {
        Map<TaskStatus, Set<TaskStatus>> transitions = new HashMap<>();
        transitions.put(PENDING, Set.of(READY));
        transitions.put(READY, Set.of(RUNNING));
        transitions.put(RUNNING, Set.of(COMPLETED, FAILED, CANCELLED));
        transitions.put(COMPLETED, Collections.emptySet());
        transitions.put(FAILED, Set.of(READY));  // 允许重试
        transitions.put(CANCELLED, Collections.emptySet());
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * 检查状态是否为终态。
     *
     * @return true 如果是终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 检查状态是否允许重试。
     *
     * @return true 如果允许重试
     */
    public boolean isRetryable() {
        return this == FAILED;
    }

    /**
     * 检查是否可以转换到目标状态。
     *
     * @param target 目标状态
     * @return true 如果可以转换
     */
    public boolean canTransitionTo(TaskStatus target) {
        if (target == null) {
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(this, Collections.emptySet()).contains(target);
    }

    /**
     * 验证状态转换是否合法。
     *
     * @param target 目标状态
     * @throws IllegalStateException 如果转换不合法
     */
    public void validateTransitionTo(TaskStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this, target)
            );
        }
    }

    /**
     * 从字符串解析状态。
     *
     * @param value 状态字符串
     * @return TaskStatus
     * @throws IllegalArgumentException 如果无效
     */
    public static TaskStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaskStatus cannot be null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TaskStatus: " + value, e);
        }
    }
}
