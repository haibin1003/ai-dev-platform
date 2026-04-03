package com.aidev.domain.model.entity;

import java.util.Objects;

/**
 * 工作流边（实体）。
 *
 * <p>表示节点之间的依赖关系。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class Edge {

    private final NodeId from;
    private final NodeId to;

    /**
     * 创建边。
     *
     * @param from 起始节点ID
     * @param to 目标节点ID
     */
    public Edge(NodeId from, NodeId to) {
        this.from = Objects.requireNonNull(from, "From node cannot be null");
        this.to = Objects.requireNonNull(to, "To node cannot be null");

        if (from.equals(to)) {
            throw new IllegalArgumentException("Self-loop is not allowed: " + from);
        }
    }

    /**
     * 创建边（便捷方法）。
     *
     * @param fromId 起始节点ID字符串
     * @param toId 目标节点ID字符串
     * @return Edge 实例
     */
    public static Edge of(String fromId, String toId) {
        return new Edge(NodeId.of(fromId), NodeId.of(toId));
    }

    public NodeId getFrom() {
        return from;
    }

    public NodeId getTo() {
        return to;
    }

    /**
     * 检查边是否包含指定节点作为起点。
     *
     * @param nodeId 节点ID
     * @return true 如果该节点是起点
     */
    public boolean isFrom(NodeId nodeId) {
        return from.equals(nodeId);
    }

    /**
     * 检查边是否包含指定节点作为终点。
     *
     * @param nodeId 节点ID
     * @return true 如果该节点是终点
     */
    public boolean isTo(NodeId nodeId) {
        return to.equals(nodeId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return String.format("Edge{%s -> %s}", from, to);
    }
}
