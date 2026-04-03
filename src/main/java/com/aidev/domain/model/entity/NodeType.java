package com.aidev.domain.model.entity;

/**
 * 节点类型枚举。
 *
 * @author AI Assistant
 * @since 1.0
 */
public enum NodeType {
    /**
     * 任务节点：执行具体任务
     */
    TASK,

    /**
     * 条件节点：根据条件选择分支（V1.5 支持）
     */
    CONDITION,

    /**
     * 并行节点：启动多个并行分支（V1.5 支持）
     */
    PARALLEL;

    /**
     * 从字符串解析类型。
     *
     * @param value 类型字符串
     * @return NodeType
     */
    public static NodeType of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NodeType cannot be null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid NodeType: " + value, e);
        }
    }
}
