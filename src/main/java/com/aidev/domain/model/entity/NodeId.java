package com.aidev.domain.model.entity;

import java.util.Objects;

/**
 * 节点标识符（值对象）。
 *
 * <p>节点ID在工作流内唯一，不需要全局唯一。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class NodeId {

    private final String value;

    private NodeId(String value) {
        this.value = Objects.requireNonNull(value, "NodeId value cannot be null");
    }

    /**
     * 从字符串创建 NodeId。
     *
     * @param value 节点ID字符串
     * @return NodeId 实例
     */
    public static NodeId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NodeId cannot be null or blank");
        }
        return new NodeId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeId)) return false;
        NodeId that = (NodeId) o;
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
