package com.aidev.domain.model.aggregate;

import com.aidev.domain.exception.CycleDetectedException;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.valueobject.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Workflow聚合根测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@DisplayName("Workflow聚合根测试")
class WorkflowTest {

    @Test
    @DisplayName("应该成功创建工作流")
    void shouldCreateWorkflow() {
        // When
        Workflow workflow = Workflow.create("测试工作流", "测试描述");

        // Then
        assertThat(workflow).isNotNull();
        assertThat(workflow.getId()).isNotNull();
        assertThat(workflow.getName()).isEqualTo("测试工作流");
        assertThat(workflow.getDescription()).isEqualTo("测试描述");
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.DRAFT);
    }

    @Test
    @DisplayName("应该添加节点到工作流")
    void shouldAddNodeToWorkflow() {
        // Given
        Workflow workflow = Workflow.create("测试工作流", null);
        Node node = Node.createTask("node1", "任务1");

        // When
        workflow.addNode(node);

        // Then
        assertThat(workflow.getNodes()).hasSize(1);
        assertThat(workflow.getNodes().get(0).getId().getValue()).isEqualTo("node1");
    }

    @Test
    @DisplayName("应该连接两个节点")
    void shouldConnectNodes() {
        // Given
        Workflow workflow = Workflow.create("测试工作流", null);
        workflow.addNode(Node.createTask("A", "任务A"));
        workflow.addNode(Node.createTask("B", "任务B"));

        // When
        workflow.connect("A", "B");

        // Then
        assertThat(workflow.getEdges()).hasSize(1);
        assertThat(workflow.getEdges().get(0).getFrom().getValue()).isEqualTo("A");
        assertThat(workflow.getEdges().get(0).getTo().getValue()).isEqualTo("B");
    }

    @Test
    @DisplayName("检测到循环依赖时应抛出异常")
    void shouldThrowExceptionWhenCycleDetected() {
        // Given
        Workflow workflow = Workflow.create("测试工作流", null);
        workflow.addNode(Node.createTask("A", "任务A"));
        workflow.addNode(Node.createTask("B", "任务B"));
        workflow.addNode(Node.createTask("C", "任务C"));
        workflow.connect("A", "B");
        workflow.connect("B", "C");
        workflow.connect("C", "A"); // 形成循环

        // When & Then
        assertThatThrownBy(workflow::validateNoCycles)
            .isInstanceOf(CycleDetectedException.class);
    }

    @Test
    @DisplayName("应该正确进行拓扑排序")
    void shouldTopologicallySortNodes() {
        // Given
        Workflow workflow = Workflow.create("测试工作流", null);
        workflow.addNode(Node.createTask("A", "任务A"));
        workflow.addNode(Node.createTask("B", "任务B"));
        workflow.addNode(Node.createTask("C", "任务C"));
        workflow.connect("A", "B");
        workflow.connect("B", "C");

        // When
        List<Node> sorted = workflow.getTopologicalOrder();

        // Then
        assertThat(sorted).hasSize(3);
        assertThat(sorted.get(0).getId().getValue()).isEqualTo("A");
        assertThat(sorted.get(1).getId().getValue()).isEqualTo("B");
        assertThat(sorted.get(2).getId().getValue()).isEqualTo("C");
    }

    @Test
    @DisplayName("应该成功激活工作流")
    void shouldActivateWorkflow() {
        // Given
        Workflow workflow = Workflow.create("测试工作流", null);
        workflow.addNode(Node.createTask("A", "任务A"));

        // When
        workflow.activate();

        // Then
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.ACTIVE);
    }

    @Test
    @DisplayName("存在循环依赖时不应允许激活")
    void shouldNotActivateWorkflowWithCycle() {
        // Given
        Workflow workflow = Workflow.create("测试工作流", null);
        workflow.addNode(Node.createTask("A", "任务A"));
        workflow.addNode(Node.createTask("B", "任务B"));
        workflow.connect("A", "B");
        workflow.connect("B", "A");

        // When & Then
        assertThatThrownBy(workflow::activate)
            .isInstanceOf(CycleDetectedException.class);
    }
}
