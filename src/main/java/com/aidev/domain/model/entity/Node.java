package com.aidev.domain.model.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流节点（实体）。
 *
 * <p>节点是工作流的组成部分，属于内部实体，没有全局唯一标识。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class Node {

    private final NodeId id;
    private String name;
    private NodeType type;
    private String agentCode;
    private Map<String, String> config;

    /**
     * 创建节点。
     *
     * @param id 节点ID
     * @param name 节点名称
     * @param type 节点类型
     */
    public Node(NodeId id, String name, NodeType type) {
        this.id = Objects.requireNonNull(id, "NodeId cannot be null");
        this.name = Objects.requireNonNull(name, "Node name cannot be null");
        this.type = Objects.requireNonNull(type, "NodeType cannot be null");
        this.agentCode = "claude-code"; // 默认Agent
        this.config = new HashMap<>();
    }

    /**
     * 创建任务节点。
     *
     * @param id 节点ID
     * @param name 节点名称
     * @return 任务节点
     */
    public static Node createTask(String id, String name) {
        return new Node(NodeId.of(id), name, NodeType.TASK);
    }

    /**
     * 设置Agent代码。
     *
     * @param agentCode Agent代码
     * @return this（链式调用）
     */
    public Node withAgent(String agentCode) {
        this.agentCode = Objects.requireNonNull(agentCode, "Agent code cannot be null");
        return this;
    }

    /**
     * 添加配置项。
     *
     * @param key 配置键
     * @param value 配置值
     * @return this（链式调用）
     */
    public Node withConfig(String key, String value) {
        this.config.put(key, value);
        return this;
    }

    public NodeId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NodeType getType() {
        return type;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public Map<String, String> getConfig() {
        return Collections.unmodifiableMap(config);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Node{id=%s, name='%s', type=%s}", id, name, type);
    }
}
