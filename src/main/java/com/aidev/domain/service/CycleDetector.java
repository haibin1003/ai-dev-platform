package com.aidev.domain.service;

import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;

import java.util.*;

/**
 * 循环检测器（领域服务）。
 *
 * <p>使用 DFS 算法检测有向图中的循环。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class CycleDetector {

    /**
     * 检测图中是否存在循环。
     *
     * @param nodes 节点列表
     * @param edges 边列表
     * @return true 如果存在循环
     */
    public static boolean hasCycle(List<Node> nodes, List<Edge> edges) {
        if (nodes.isEmpty() || edges.isEmpty()) {
            return false;
        }

        // 构建邻接表
        Map<NodeId, List<NodeId>> adjacency = buildAdjacencyList(nodes, edges);

        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> recursionStack = new HashSet<>();

        for (Node node : nodes) {
            if (dfsHasCycle(node.getId(), adjacency, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 查找循环路径。
     *
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 循环路径（如果存在），否则返回空列表
     */
    public static List<String> findCycle(List<Node> nodes, List<Edge> edges) {
        if (nodes.isEmpty() || edges.isEmpty()) {
            return Collections.emptyList();
        }

        Map<NodeId, List<NodeId>> adjacency = buildAdjacencyList(nodes, edges);
        Set<NodeId> visited = new HashSet<>();
        Set<NodeId> recursionStack = new HashSet<>();
        List<NodeId> path = new ArrayList<>();

        for (Node node : nodes) {
            List<NodeId> cycle = dfsFindCycle(node.getId(), adjacency, visited, recursionStack, path);
            if (!cycle.isEmpty()) {
                return cycle.stream()
                    .map(NodeId::getValue)
                    .toList();
            }
        }

        return Collections.emptyList();
    }

    // ==================== 私有方法 ====================

    private static Map<NodeId, List<NodeId>> buildAdjacencyList(List<Node> nodes, List<Edge> edges) {
        Map<NodeId, List<NodeId>> adjacency = new HashMap<>();

        // 初始化所有节点的邻接列表
        for (Node node : nodes) {
            adjacency.put(node.getId(), new ArrayList<>());
        }

        // 添加边
        for (Edge edge : edges) {
            adjacency.get(edge.getFrom()).add(edge.getTo());
        }

        return adjacency;
    }

    private static boolean dfsHasCycle(NodeId nodeId,
                                       Map<NodeId, List<NodeId>> adjacency,
                                       Set<NodeId> visited,
                                       Set<NodeId> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }

        if (visited.contains(nodeId)) {
            return false;
        }

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

    private static List<NodeId> dfsFindCycle(NodeId nodeId,
                                             Map<NodeId, List<NodeId>> adjacency,
                                             Set<NodeId> visited,
                                             Set<NodeId> recursionStack,
                                             List<NodeId> path) {
        if (recursionStack.contains(nodeId)) {
            // 找到循环，提取循环路径
            int cycleStart = path.indexOf(nodeId);
            List<NodeId> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
            cycle.add(nodeId);
            return cycle;
        }

        if (visited.contains(nodeId)) {
            return Collections.emptyList();
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);
        path.add(nodeId);

        for (NodeId neighbor : adjacency.getOrDefault(nodeId, List.of())) {
            List<NodeId> cycle = dfsFindCycle(neighbor, adjacency, visited, recursionStack, path);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(nodeId);
        return Collections.emptyList();
    }
}
