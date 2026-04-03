package com.aidev.domain.model.valueobject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 工作流状态（值对象）。
 *
 * <p>不可变枚举，定义状态转换规则。
 *
 * @author AI Assistant
 * @since 1.0
 */
public enum WorkflowStatus {
    /**
     * 草稿状态：可编辑，不可执行
     */
    DRAFT,

    /**
     * 激活状态：可执行
     */
    ACTIVE,

    /**
     * 已归档：不可编辑，不可执行
     */
    ARCHIVED;

    // 状态转换规则定义
    private static final Map<WorkflowStatus, Set<WorkflowStatus>> VALID_TRANSITIONS;

    static {
        Map<WorkflowStatus, Set<WorkflowStatus>> transitions = new HashMap<>();
        transitions.put(DRAFT, Set.of(ACTIVE, ARCHIVED));
        transitions.put(ACTIVE, Set.of(ARCHIVED));
        transitions.put(ARCHIVED, Collections.emptySet());
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * 检查是否可以转换到目标状态。
     *
     * @param target 目标状态
     * @return true 如果可以转换
     */
    public boolean canTransitionTo(WorkflowStatus target) {
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
    public void validateTransitionTo(WorkflowStatus target) {
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
     * @return WorkflowStatus
     * @throws IllegalArgumentException 如果无效
     */
    public static WorkflowStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WorkflowStatus cannot be null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid WorkflowStatus: " + value, e);
        }
    }
}
