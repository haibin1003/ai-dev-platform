package com.aidev.domain.model.aggregate;

import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Execution聚合根测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@DisplayName("Execution聚合根测试")
class ExecutionTest {

    @Test
    @DisplayName("应该成功创建执行实例")
    void shouldCreateExecution() {
        // When
        Execution execution = Execution.create(
            WorkflowId.generate(),
            Map.of("key", "value")
        );

        // Then
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isNotNull();
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.PENDING);
        assertThat(execution.getTasks()).isEmpty();
    }

    @Test
    @DisplayName("应该添加任务到执行实例")
    void shouldAddTaskToExecution() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task = Task.create(
            execution.getId(),
            WorkflowId.generate(),
            NodeId.of("node1"),
            null,
            3
        );

        // When
        execution.addTask(task);

        // Then
        assertThat(execution.getTasks()).hasSize(1);
        assertThat(execution.getTotalTaskCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("开始执行应该改变状态为RUNNING")
    void shouldStartExecution() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);

        // When
        execution.start();

        // Then
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.RUNNING);
        assertThat(execution.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("任务完成应该增加已完成任务计数")
    void shouldIncrementCompletedCount() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task = createCompletedTask(execution.getId());
        execution.addTask(task);

        // When
        execution.taskCompleted(task.getId());

        // Then
        assertThat(execution.getCompletedTaskCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("任务失败应该增加失败任务计数")
    void shouldIncrementFailedCount() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task = createFailedTask(execution.getId());
        execution.addTask(task);

        // When
        execution.taskFailed(task.getId());

        // Then
        assertThat(execution.getFailedTaskCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("所有任务完成后执行应该为COMPLETED状态")
    void shouldBeCompletedWhenAllTasksDone() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task1 = createCompletedTask(execution.getId());
        Task task2 = createCompletedTask(execution.getId());
        execution.addTask(task1);
        execution.addTask(task2);

        // When
        execution.taskCompleted(task1.getId());
        execution.taskCompleted(task2.getId());

        // Then
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(execution.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("有任务失败且无法重试时执行应该为FAILED状态")
    void shouldBeFailedWhenTaskFailedAndNotRetryable() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task = createFailedTask(execution.getId());
        task.retry();
        task.start();
        task.fail("Second failure");
        execution.addTask(task);

        // When
        execution.taskFailed(task.getId());

        // Then
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    @DisplayName("应该根据状态筛选任务")
    void shouldFilterTasksByStatus() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task completedTask = createCompletedTask(execution.getId());
        Task pendingTask = Task.create(
            execution.getId(),
            WorkflowId.generate(),
            NodeId.of("pending"),
            null,
            3
        );
        execution.addTask(completedTask);
        execution.addTask(pendingTask);

        // When
        var completedTasks = execution.getTasksByStatus(TaskStatus.COMPLETED);
        var pendingTasks = execution.getTasksByStatus(TaskStatus.PENDING);

        // Then
        assertThat(completedTasks).hasSize(1);
        assertThat(pendingTasks).hasSize(1);
    }

    @Test
    @DisplayName("取消执行应该改变状态为CANCELLED")
    void shouldCancelExecution() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);

        // When
        execution.cancel();

        // Then
        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
    }

    @Test
    @DisplayName("已完成的执行不应该允许添加任务")
    void shouldNotAllowAddTaskToCompletedExecution() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task = createCompletedTask(execution.getId());
        execution.addTask(task);
        execution.taskCompleted(task.getId());

        // Then
        assertThatThrownBy(() -> execution.addTask(createCompletedTask(execution.getId())))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("所有任务完成后isComplete应该返回true")
    void isCompleteShouldReturnTrueWhenAllTasksTerminal() {
        // Given
        Execution execution = Execution.create(WorkflowId.generate(), null);
        Task task = createCompletedTask(execution.getId());
        execution.addTask(task);

        // When
        execution.taskCompleted(task.getId());

        // Then
        assertThat(execution.isComplete()).isTrue();
    }

    // Helper methods
    private Task createCompletedTask(ExecutionId executionId) {
        Task task = Task.create(
            executionId,
            WorkflowId.generate(),
            NodeId.of("node1"),
            null,
            3
        );
        task.markReady();
        task.start();
        task.complete(ExecutionResult.builder()
            .exitCode(0)
            .output("success")
            .durationMs(1000)
            .build());
        return task;
    }

    private Task createFailedTask(ExecutionId executionId) {
        Task task = Task.create(
            executionId,
            WorkflowId.generate(),
            NodeId.of("node1"),
            null,
            1
        );
        task.markReady();
        task.start();
        task.fail("error");
        return task;
    }
}
