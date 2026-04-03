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
 * CycleDetector 领域服务测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@DisplayName("CycleDetector 循环检测测试")
class CycleDetectorTest {

    @Test
    @DisplayName("空图应该无循环")
    void shouldReturnNoCycleForEmptyGraph() {
        // When
        boolean hasCycle = CycleDetector.hasCycle(Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(hasCycle).isFalse();
    }

    @Test
    @DisplayName("单节点应该无循环")
    void shouldReturnNoCycleForSingleNode() {
        // Given
        Node node = createNode("A");

        // When
        boolean hasCycle = CycleDetector.hasCycle(List.of(node), Collections.emptyList());

        // Then
        assertThat(hasCycle).isFalse();
    }

    @Test
    @DisplayName("线性依赖应该无循环")
    void shouldReturnNoCycleForLinearDependencies() {
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
        boolean hasCycle = CycleDetector.hasCycle(nodes, edges);

        // Then
        assertThat(hasCycle).isFalse();
    }

    @Test
    @DisplayName("简单三角循环应该检测到")
    void shouldDetectSimpleTriangleCycle() {
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

        // When
        boolean hasCycle = CycleDetector.hasCycle(nodes, edges);

        // Then
        assertThat(hasCycle).isTrue();
    }

    @Test
    @DisplayName("复杂DAG应该无循环")
    void shouldReturnNoCycleForComplexDAG() {
        // Given:
        //   A -> B -> D
        //   A -> C -> D
        //   B -> E
        //   C -> F
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        Node nodeE = createNode("E");
        Node nodeF = createNode("F");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD, nodeE, nodeF);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("A", "C"),
            createEdge("B", "D"),
            createEdge("C", "D"),
            createEdge("B", "E"),
            createEdge("C", "F")
        );

        // When
        boolean hasCycle = CycleDetector.hasCycle(nodes, edges);

        // Then
        assertThat(hasCycle).isFalse();
    }

    @Test
    @DisplayName("复杂循环应该检测到")
    void shouldDetectComplexCycle() {
        // Given:
        //   A -> B -> C -> D
        //   D -> B (形成循环 B -> C -> D -> B)
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("B", "C"),
            createEdge("C", "D"),
            createEdge("D", "B")
        );

        // When
        boolean hasCycle = CycleDetector.hasCycle(nodes, edges);

        // Then
        assertThat(hasCycle).isTrue();
    }

    @Test
    @DisplayName("多个独立循环应该检测到")
    void shouldDetectMultipleCycles() {
        // Given:
        //   循环1: A -> B -> A
        //   循环2: C -> D -> C
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("B", "A"),
            createEdge("C", "D"),
            createEdge("D", "C")
        );

        // When
        boolean hasCycle = CycleDetector.hasCycle(nodes, edges);

        // Then
        assertThat(hasCycle).isTrue();
    }

    @Test
    @DisplayName("无边图应该无循环")
    void shouldReturnNoCycleForNoEdges() {
        // Given: 多个节点，没有边
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC);

        // When
        boolean hasCycle = CycleDetector.hasCycle(nodes, Collections.emptyList());

        // Then
        assertThat(hasCycle).isFalse();
    }

    // ==================== findCycle 测试 ====================

    @Test
    @DisplayName("无循环时findCycle应该返回空列表")
    void shouldReturnEmptyListWhenNoCycle() {
        // Given
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        List<Node> nodes = List.of(nodeA, nodeB);
        List<Edge> edges = List.of(createEdge("A", "B"));

        // When
        List<String> cycle = CycleDetector.findCycle(nodes, edges);

        // Then
        assertThat(cycle).isEmpty();
    }

    @Test
    @DisplayName("应该找到三角循环路径")
    void shouldFindTriangleCyclePath() {
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

        // When
        List<String> cycle = CycleDetector.findCycle(nodes, edges);

        // Then
        assertThat(cycle).isNotEmpty();
        // 循环路径应该包含 A, B, C
        assertThat(cycle).contains("A", "B", "C");
    }

    @Test
    @DisplayName("应该找到复杂循环路径")
    void shouldFindComplexCyclePath() {
        // Given: A -> B -> C -> D -> B
        Node nodeA = createNode("A");
        Node nodeB = createNode("B");
        Node nodeC = createNode("C");
        Node nodeD = createNode("D");
        List<Node> nodes = List.of(nodeA, nodeB, nodeC, nodeD);
        List<Edge> edges = List.of(
            createEdge("A", "B"),
            createEdge("B", "C"),
            createEdge("C", "D"),
            createEdge("D", "B")
        );

        // When
        List<String> cycle = CycleDetector.findCycle(nodes, edges);

        // Then: 循环路径应该包含 B, C, D
        assertThat(cycle).isNotEmpty();
        assertThat(cycle).contains("B", "C", "D");
        assertThat(cycle).doesNotContain("A"); // A 不在循环中
    }

    @Test
    @DisplayName("空图findCycle应该返回空列表")
    void shouldReturnEmptyListForEmptyGraph() {
        // When
        List<String> cycle = CycleDetector.findCycle(Collections.emptyList(), Collections.emptyList());

        // Then
        assertThat(cycle).isEmpty();
    }

    // Helper methods
    private Node createNode(String id) {
        return Node.createTask(id, "Node " + id);
    }

    private Edge createEdge(String from, String to) {
        return new Edge(NodeId.of(from), NodeId.of(to));
    }
}
