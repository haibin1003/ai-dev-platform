package com.aidev.domain.model.valueobject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 执行实例状态（值对象）。
 *
 * <p>不可变枚举。
 *
 * @author AI Assistant
 * @since 1.0
 */
public enum ExecutionStatus {
    /**
     * 待执行：已创建，等待开始
     */
    PENDING,

    /**
     * 运行中：至少有一个任务在执行
     */
    RUNNING,

    /**
     * 已完成：所有任务成功完成
     */
    COMPLETED,

    /**
     * 已失败：至少有一个任务失败且无法重试
     */
    FAILED,

    /**
     * 已取消：手动取消
     */
    CANCELLED;

    // 状态转换规则
    private static final Map<ExecutionStatus, Set<ExecutionStatus>> VALID_TRANSITIONS;

    static {
        Map<ExecutionStatus, Set<ExecutionStatus>> transitions = new HashMap<>();
        transitions.put(PENDING, Set.of(RUNNING, CANCELLED));
        transitions.put(RUNNING, Set.of(COMPLETED, FAILED, CANCELLED));
        transitions.put(COMPLETED, Collections.emptySet());
        transitions.put(FAILED, Collections.emptySet());
        transitions.put(CANCELLED, Collections.emptySet());
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * 检查状态是否为终态。
     *
     * @return true 如果是终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * 检查是否可以转换到目标状态。
     *
     * @param target 目标状态
     * @return true 如果可以转换
     */
    public boolean canTransitionTo(ExecutionStatus target) {
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
    public void validateTransitionTo(ExecutionStatus target) {
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
     * @return ExecutionStatus
     * @throws IllegalArgumentException 如果无效
     */
    public static ExecutionStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ExecutionStatus cannot be null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ExecutionStatus: " + value, e);
        }
    }
}
