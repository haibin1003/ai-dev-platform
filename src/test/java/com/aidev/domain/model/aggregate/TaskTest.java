package com.aidev.domain.model.aggregate;

import com.aidev.domain.event.TaskCompletedEvent;
import com.aidev.domain.event.TaskFailedEvent;
import com.aidev.domain.event.TaskStartedEvent;
import com.aidev.domain.exception.InvalidStatusTransitionException;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Task聚合根测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@DisplayName("Task聚合根测试")
class TaskTest {

    @Test
    @DisplayName("应该成功创建任务")
    void shouldCreateTask() {
        // When
        Task task = Task.create(
            ExecutionId.generate(),
            WorkflowId.generate(),
            NodeId.of("node1"),
            Map.of("key", "value"),
            3
        );

        // Then
        assertThat(task).isNotNull();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getRetryCount()).isZero();
        assertThat(task.getMaxRetries()).isEqualTo(3);
    }

    @Test
    @DisplayName("应该从PENDING转换到READY")
    void shouldTransitionFromPendingToReady() {
        // Given
        Task task = createPendingTask();

        // When
        task.markReady();

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.READY);
    }

    @Test
    @DisplayName("应该从READY转换到RUNNING")
    void shouldTransitionFromReadyToRunning() {
        // Given
        Task task = createPendingTask();
        task.markReady();

        // When
        task.start();

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.RUNNING);
        assertThat(task.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("启动任务应该记录TaskStartedEvent")
    void shouldRecordTaskStartedEvent() {
        // Given
        Task task = createPendingTask();
        task.markReady();
        task.drainEvents(); // 清除之前的事件

        // When
        task.start();

        // Then
        var events = task.drainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TaskStartedEvent.class);
    }

    @Test
    @DisplayName("应该从RUNNING转换到COMPLETED")
    void shouldTransitionFromRunningToCompleted() {
        // Given
        Task task = createRunningTask();
        ExecutionResult result = ExecutionResult.builder()
            .exitCode(0)
            .output("success")
            .durationMs(1000)
            .build();

        // When
        task.complete(result);

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getResult()).isNotNull();
        assertThat(task.getResult().isSuccess()).isTrue();
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("完成任务应该记录TaskCompletedEvent")
    void shouldRecordTaskCompletedEvent() {
        // Given
        Task task = createRunningTask();
        task.drainEvents(); // 清除之前的事件
        ExecutionResult result = ExecutionResult.builder()
            .exitCode(0)
            .output("success")
            .durationMs(1000)
            .build();

        // When
        task.complete(result);

        // Then
        var events = task.drainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TaskCompletedEvent.class);
    }

    @Test
    @DisplayName("应该从RUNNING转换到FAILED")
    void shouldTransitionFromRunningToFailed() {
        // Given
        Task task = createRunningTask();

        // When
        task.fail("Execution failed");

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("Execution failed");
    }

    @Test
    @DisplayName("失败任务应该记录TaskFailedEvent")
    void shouldRecordTaskFailedEvent() {
        // Given
        Task task = createRunningTask();
        task.drainEvents(); // 清除之前的事件

        // When
        task.fail("Execution failed");

        // Then
        var events = task.drainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(TaskFailedEvent.class);
    }

    @Test
    @DisplayName("失败且可重试的任务应该允许重试")
    void shouldAllowRetryForFailedTask() {
        // Given
        Task task = createRunningTask();
        task.fail("Execution failed");

        // Then
        assertThat(task.canRetry()).isTrue();
    }

    @Test
    @DisplayName("重试次数达到上限后不应允许重试")
    void shouldNotAllowRetryWhenMaxRetriesReached() {
        // Given
        Task task = Task.create(
            ExecutionId.generate(),
            WorkflowId.generate(),
            NodeId.of("node1"),
            null,
            1
        );
        task.markReady();
        task.start();
        task.fail("First failure");
        task.retry();
        task.start();
        task.fail("Second failure");

        // Then
        assertThat(task.canRetry()).isFalse();
    }

    @Test
    @DisplayName("重试后应该重置状态为READY")
    void shouldResetStatusToReadyOnRetry() {
        // Given
        Task task = createRunningTask();
        task.fail("Execution failed");

        // When
        task.retry();

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.READY);
        assertThat(task.getRetryCount()).isEqualTo(1);
        assertThat(task.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("非法状态转换应该抛出异常")
    void shouldThrowExceptionForInvalidTransition() {
        // Given
        Task task = createPendingTask();

        // Then
        assertThatThrownBy(task::start)
            .isInstanceOf(InvalidStatusTransitionException.class)
            .hasMessageContaining("Invalid status transition");
    }

    @Test
    @DisplayName("完成的任务应该是终态")
    void completedTaskShouldBeTerminal() {
        // Given
        Task task = createRunningTask();
        task.complete(ExecutionResult.builder()
            .exitCode(0)
            .output("success")
            .durationMs(1000)
            .build());

        // Then
        assertThat(task.getStatus().isTerminal()).isTrue();
    }

    @Test
    @DisplayName("取消任务应该改变状态为CANCELLED")
    void shouldCancelTask() {
        // Given
        Task task = createRunningTask();

        // When
        task.cancel();

        // Then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    // Helper methods
    private Task createPendingTask() {
        return Task.create(
            ExecutionId.generate(),
            WorkflowId.generate(),
            NodeId.of("node1"),
            Map.of("key", "value"),
            3
        );
    }

    private Task createRunningTask() {
        Task task = createPendingTask();
        task.markReady();
        task.start();
        return task;
    }
}
