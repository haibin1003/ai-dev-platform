package com.aidev.domain.service;

import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TopologicalSorter 领域服务测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@DisplayName("TopologicalSorter 拓扑排序测试")
class TopologicalSorterTest {

    @Test
    @DisplayName("空图应该返回空列表")
    void shouldReturnEmptyForEmptyGraph() {
        // When
        List<Node> result = TopologicalSorter.sort(Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("单节点图应该返回该节点")
    void shouldReturnSingleNode() {
        // Given
        Node node = createNode("node1");

        // When
        List<Node> result = TopologicalSorter.sort(List.of(node), Collections.emptyList());

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().getValue()).isEqualTo("node1");
    }

    @Test
    @DisplayName("线性依赖应该正确排序")
    void shouldSortLinearDependencies() {
        // Given: A -> B -> C
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("B", "C")
        );

        // When
        List<Node> result = TopologicalSorter.sort(nodes, edges);

        // Then: 顺序必须是 A, B, C
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId().getValue()).isEqualTo("A");
        assertThat(result.get(1).getId().getValue()).isEqualTo("B");
        assertThat(result.get(2).getId().getValue()).isEqualTo("C");
    }

    @Test
    @DisplayName("多入度节点应该排在所有依赖之后")
    void shouldSortMultipleDependencies() {
        // Given: A -> C, B -> C (C 依赖 A 和 B)
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC);
        List<Edge> edges = List.of(
            createEdge("A", "C"),
            createEdge("B", "C")
        );

        // When
        List<Node> result = TopologicalSorter.sort(nodes, edges);

        // Then: C 必须在 A 和 B 之后
        assertThat(result).hasSize(3);
        int indexA = findIndex(result, "A");
        int indexB = findIndex(result, "B");
        int indexC = findIndex(result, "C");
        assertThat(indexC).isGreaterThan(indexA);
        assertThat(indexC).isGreaterThan(indexB);
    }

    @Test
    @DisplayName("并行节点可以任意顺序")
    void shouldHandleParallelNodes() {
        // Given: A -> C, B -> D (A和B无依赖，C和D无交叉依赖)
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<Edge> edges = List.of(
            createEdge("A", "C"),
            createEdge("B", "D")
        );

        // When
        List<Node> result = TopologicalSorter.sort(nodes, edges);

        // Then: A在C前，B在D前
        assertThat(result).hasSize(4);
        assertThat(findIndex(result, "A")).isLessThan(findIndex(result, "C"));
        assertThat(findIndex(result, "B")).isLessThan(findIndex(result, "D"));
    }

    @Test
    @DisplayName("有循环依赖应该抛出异常")
    void shouldThrowExceptionForCycle() {
        // Given: A -> B -> C -> A
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("B", "C"),
            createEdge("C", "A")
        );

        // Then
        assertThatThrownBy(() -> TopologicalSorter.sort(nodes, edges))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cycle detected");
    }

    @Test
    @DisplayName("复杂DAG应该正确排序")
    void shouldSortComplexDAG() {
        // Given:
        //   A -> B -> D
        //   A -> C -> D
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("A", "C"),
            createEdge("B", "D"),
            createEdge("C", "D")
        );

        // When
        List<Node> result = TopologicalSorter.sort(nodes, edges);

        // Then: A 在最前，D 在最后
        assertThat(result).hasSize(4);
        assertThat(result.get(0).getId().getValue()).isEqualTo("A");
        assertThat(result.get(3).getId().getValue()).isEqualTo("D");
        // B 和 C 在中间，顺序不定
        assertThat(findIndex(result, "B")).isGreaterThan(0).isLessThan(3);
        assertThat(findIndex(result, "C")).isGreaterThan(0).isLessThan(3);
    }

    // ==================== getParallelGroups 测试 ====================

    @Test
    @DisplayName("空图应该返回空并行组")
    void shouldReturnEmptyGroupsForEmptyGraph() {
        // When
        List<List<Node>> groups = TopologicalSorter.getParallelGroups(
            Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(groups).isEmpty();
    }

    @Test
    @DisplayName("单节点应该返回单组")
    void shouldReturnSingleGroupForSingleNode() {
        // Given
        Node node = createNode("node1");

        // When
        List<List<Node>> groups = TopologicalSorter.getParallelGroups(
            List.of(node), Collections.emptyList());

        // Then
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).hasSize(1);
    }

    @Test
    @DisplayName("无依赖节点应该分到同一组")
    void shouldGroupIndependentNodesTogether() {
        // Given: A, B, C 无依赖
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC);

        // When
        List<List<Node>> groups = TopologicalSorter.getParallelGroups(nodes, Collections.emptyList());

        // Then: 所有节点在第一组
        assertThat(groups).hasSize(1);
        assertThat(groups.get(0)).hasSize(3);
    }

    @Test
    @DisplayName("线性依赖应该每组一个节点")
    void shouldCreateSeparateGroupsForLinearDependencies() {
        // Given: A -> B -> C
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("B", "C")
        );

        // When
        List<List<Node>> groups = TopologicalSorter.getParallelGroups(nodes, edges);

        // Then: 三组，每组一个节点
        assertThat(groups).hasSize(3);
        assertThat(groups.get(0).get(0).getId().getValue()).isEqualTo("A");
        assertThat(groups.get(1).get(0).getId().getValue()).isEqualTo("B");
        assertThat(groups.get(2).get(0).getId().getValue()).isEqualTo("C");
    }

    @Test
    @DisplayName("并行执行组应该正确分组")
    void shouldCreateCorrectParallelGroups() {
        // Given:
        //   第一层: A, B
        //   第二层: C (依赖 A, B)
        //   第三层: D (依赖 C)
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<Edge> edges = List.of(
            createEdge("A", "C"),
            createEdge("B", "C"),
            createEdge("C", "D")
        );

        // When
        List<List<Node>> groups = TopologicalSorter.getParallelGroups(nodes, edges);

        // Then: 3组 - [A,B], [C], [D]
        assertThat(groups).hasSize(3);
        assertThat(groups.get(0)).hasSize(2); // A, B 可以并行
        assertThat(groups.get(1)).hasSize(1); // C
        assertThat(groups.get(2)).hasSize(1); // D
    }

    // Helper methods
    private Node createNode(String id) {
        return Node.createTask(id, "Node " + id);
    }

    private Edge createEdge(String from, String to) {
        return new Edge(NodeId.of(from), NodeId.of(to));
    }

    private int findIndex(List<Node> nodes, String nodeId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().getValue().equals(nodeId)) {
                return i;
            }
        }
        return -1;
    }
}
