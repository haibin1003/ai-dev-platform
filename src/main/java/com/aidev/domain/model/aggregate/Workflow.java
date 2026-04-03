package com.aidev.domain.model.aggregate;

import com.aidev.domain.event.DomainEvent;
import com.aidev.domain.exception.CycleDetectedException;
import com.aidev.domain.exception.InvalidStatusTransitionException;
import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流聚合根。
 *
 * <p>负责管理工作流定义，包含节点、边和变量。
 * 维护聚合边界内的一致性，处理拓扑排序和循环检测。
 *
 * @author AI Assistant
 * @since 1.0
 * @see Node
 * @see Edge
 */
public class Workflow {

    private final WorkflowId id;
    private String name;
    private String description;
    private WorkflowStatus status;
    private final List<Node> nodes;
    private final List<Edge> edges;
    private final Map<String, String> variables;
    private final List<DomainEvent> domainEvents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 私有构造函数，使用工厂方法创建。
     */
    private Workflow(WorkflowId id, String name, String description) {
        this.id = Objects.requireNonNull(id, "WorkflowId cannot be null");
        this.name = Objects.requireNonNull(name, "Workflow name cannot be null");
        this.description = description;
        this.status = WorkflowStatus.DRAFT;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.variables = new HashMap<>();
        this.domainEvents = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 创建工作流。
     *
     * @param name 工作流名称
     * @param description 工作流描述
     * @return 新的工作流实例
     */
    public static Workflow create(String name, String description) {
        return new Workflow(WorkflowId.generate(), name, description);
    }

    /**
     * 从已存在的ID创建工作流（用于重构）。
     *
     * @param id 工作流ID
     * @param name 工作流名称
     * @param description 工作流描述
     * @return 工作流实例
     */
    public static Workflow of(WorkflowId id, String name, String description) {
        return new Workflow(id, name, description);
    }

    // ==================== 核心行为 ====================

    /**
     * 添加节点。
     *
     * @param node 要添加的节点
     * @throws IllegalArgumentException 如果节点ID已存在
     */
    public void addNode(Node node) {
        Objects.requireNonNull(node, "Node cannot be null");

        if (findNodeById(node.getId()).isPresent()) {
            throw new IllegalArgumentException("Node with id " + node.getId() + " already exists");
        }

        nodes.add(node);
        touch();
    }

    /**
     * 连接两个节点。
     *
     * @param fromId 起始节点ID
     * @param toId 目标节点ID
     * @throws IllegalArgumentException 如果节点不存在或边已存在
     */
    public void connect(String fromId, String toId) {
        NodeId from = NodeId.of(fromId);
        NodeId to = NodeId.of(toId);

        // 验证节点存在
        if (findNodeById(from).isEmpty()) {
            throw new IllegalArgumentException("Source node not found: " + fromId);
        }
        if (findNodeById(to).isEmpty()) {
            throw new IllegalArgumentException("Target node not found: " + toId);
        }

        // 检查边是否已存在
        Edge newEdge = new Edge(from, to);
        if (edges.contains(newEdge)) {
            throw new IllegalArgumentException("Edge already exists: " + fromId + " -> " + toId);
        }

        edges.add(newEdge);
        touch();
    }

    /**
     * 验证无循环依赖。
     *
     * @throws CycleDetectedException 如果检测到循环
     */
    public void validateNoCycles() {
        if (hasCycle()) {
            throw new CycleDetectedException();
        }
    }

    /**
     * 获取拓扑排序后的节点列表。
     *
     * @return 按执行顺序排列的节点列表
     * @throws CycleDetectedException 如果存在循环依赖
     */
    public List<Node> getTopologicalOrder() {
        if (hasCycle()) {
            throw new CycleDetectedException();
        }
        return topologicalSort();
    }

    /**
     * 激活工作流。
     *
     * @throws InvalidStatusTransitionException 如果状态不允许激活
     */
    public void activate() {
        status.validateTransitionTo(WorkflowStatus.ACTIVE);
        validateNoCycles();
        this.status = WorkflowStatus.ACTIVE;
        touch();
    }

    /**
     * 归档工作流。
     *
     * @throws InvalidStatusTransitionException 如果状态不允许归档
     */
    public void archive() {
        status.validateTransitionTo(WorkflowStatus.ARCHIVED);
        this.status = WorkflowStatus.ARCHIVED;
        touch();
    }

    /**
     * 设置变量。
     *
     * @param key 变量名
     * @param value 变量值
     */
    public void setVariable(String key, String value) {
        Objects.requireNonNull(key, "Variable key cannot be null");
        this.variables.put(key, value);
        touch();
    }

    // ==================== 查询方法 ====================

    public Optional<Node> findNodeById(NodeId id) {
        return nodes.stream()
            .filter(n -> n.getId().equals(id))
            .findFirst();
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Edge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Map<String, String> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    // ==================== 私有方法 ====================

    private boolean hasCycle() {
        // 构建邻接表
        Map<NodeId, List<NodeId>> adjacency = new HashMap<>();
        for (Node node : nodes) {
            adjacency.put(node.getId(), new ArrayList<>());
        }
        for (Edge edge : edges) {
            adjacency.get(edge.getFrom()).add(edge.getTo());
        }

        // DFS 检测循环
        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> recursionStack = new HashSet<>();

        for (Node node : nodes) {
            if (dfsHasCycle(node.getId(), adjacency, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsHasCycle(NodeId nodeId, Map<NodeId, List<NodeId>> adjacency,
                                 Set<NodeId> visited, Set<NodeId> recursionStack) {
        if (recursionStack.contains(nodeId)) return true;
        if (visited.contains(nodeId)) return false;

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (NodeId neighbor : adjacency.getOrDefault(nodeId, List.of())) {
            if (dfsHasCycle(neighbor, adjacency, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    private List<Node> topologicalSort() {
        // 计算入度
        Map<NodeId, Integer> inDegree = nodes.stream()
            .collect(Collectors.toMap(Node::getId, n -> 0));

        for (Edge edge : edges) {
            inDegree.merge(edge.getTo(), 1, Integer::sum);
        }

        // Kahn 算法
        Queue<NodeId> queue = nodes.stream()
            .filter(n -> inDegree.get(n.getId()) == 0)
            .map(Node::getId)
            .collect(Collectors.toCollection(LinkedList::new));

        List<Node> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            NodeId currentId = queue.poll();
            findNodeById(currentId).ifPresent(result::add);

            for (Edge edge : edges) {
                if (edge.getFrom().equals(currentId)) {
                    NodeId dependent = edge.getTo();
                    inDegree.merge(dependent, -1, Integer::sum);
                    if (inDegree.get(dependent) == 0) {
                        queue.offer(dependent);
                    }
                }
            }
        }

        return result;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Getter ====================

    public WorkflowId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    /**
     * 检查工作流是否可以执行。
     *
     * @return true 如果工作流处于ACTIVE状态
     */
    public boolean canExecute() {
        return status == WorkflowStatus.ACTIVE;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
