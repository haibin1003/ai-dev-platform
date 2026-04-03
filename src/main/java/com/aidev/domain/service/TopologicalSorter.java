package com.aidev.domain.service;

import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 拓扑排序器（领域服务）。
 *
 * <p>使用 Kahn 算法对有向无环图进行拓扑排序。
 *
 * @author AI Assistant
 * @since 1.0
 * @see com.aidev.domain.model.aggregate.Workflow
 */
public class TopologicalSorter {

    /**
     * 对节点进行拓扑排序。
     *
     * <p>使用 Kahn 算法：
     * <ol>
     *   <li>计算每个节点的入度</li>
     *   <li>将入度为 0 的节点加入队列</li>
     *   <li>依次取出节点，更新其邻居节点的入度</li>
     *   <li>重复直到队列为空</li>
     * </ol>
     *
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 按执行顺序排列的节点列表
     * @throws IllegalStateException 如果存在循环依赖
     */
    public static List<Node> sort(List<Node> nodes, List<Edge> edges) {
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建节点ID到节点的映射
        Map<NodeId, Node> nodeMap = nodes.stream()
            .collect(Collectors.toMap(Node::getId, n -> n));

        // 计算入度
        Map<NodeId, Integer> inDegree = nodes.stream()
            .collect(Collectors.toMap(Node::getId, n -> 0));

        for (Edge edge : edges) {
            inDegree.merge(edge.getTo(), 1, Integer::sum);
        }

        // 入度为 0 的节点入队
        Queue<NodeId> queue = new LinkedList<>();
        for (Map.Entry<NodeId, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Kahn 算法
        List<Node> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            NodeId currentId = queue.poll();
            Node currentNode = nodeMap.get(currentId);
            result.add(currentNode);

            // 更新邻居节点的入度
            for (Edge edge : edges) {
                if (edge.getFrom().equals(currentId)) {
                    NodeId dependent = edge.getTo();
                    int newDegree = inDegree.get(dependent) - 1;
                    inDegree.put(dependent, newDegree);
                    if (newDegree == 0) {
                        queue.offer(dependent);
                    }
                }
            }
        }

        // 检测循环
        if (result.size() != nodes.size()) {
            throw new IllegalStateException("Cycle detected in graph");
        }

        return result;
    }

    /**
     * 获取可以并行执行的节点组。
     *
     * <p>每一组内的节点可以并行执行，组间有依赖关系。
     *
     * @param nodes 节点列表
     * @param edges 边列表
     * @return 并行组列表
     */
    public static List<List<Node>> getParallelGroups(List<Node> nodes, List<Edge> edges) {
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<NodeId, Node> nodeMap = nodes.stream()
            .collect(Collectors.toMap(Node::getId, n -> n));

        Map<NodeId, Integer> inDegree = nodes.stream()
            .collect(Collectors.toMap(Node::getId, n -> 0));

        for (Edge edge : edges) {
            inDegree.merge(edge.getTo(), 1, Integer::sum);
        }

        List<List<Node>> groups = new ArrayList<>();
        Queue<NodeId> queue = new LinkedList<>();

        // 初始化：找到所有入度为 0 的节点
        for (Map.Entry<NodeId, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            // 当前层的所有节点可以并行执行
            List<Node> currentGroup = new ArrayList<>();
            int size = queue.size();

            for (int i = 0; i < size; i++) {
                NodeId currentId = queue.poll();
                currentGroup.add(nodeMap.get(currentId));

                for (Edge edge : edges) {
                    if (edge.getFrom().equals(currentId)) {
                        NodeId dependent = edge.getTo();
                        int newDegree = inDegree.get(dependent) - 1;
                        inDegree.put(dependent, newDegree);
                        if (newDegree == 0) {
                            queue.offer(dependent);
                        }
                    }
                }
            }

            groups.add(currentGroup);
        }

        return groups;
    }
}
