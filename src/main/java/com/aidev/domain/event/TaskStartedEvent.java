package com.aidev.domain.event;

import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.TaskId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 任务开始事件。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class TaskStartedEvent implements DomainEvent {

    private final TaskId taskId;
    private final ExecutionId executionId;
    private final LocalDateTime occurredAt;

    public TaskStartedEvent(TaskId taskId, ExecutionId executionId) {
        this.taskId = Objects.requireNonNull(taskId, "TaskId cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "ExecutionId cannot be null");
        this.occurredAt = LocalDateTime.now();
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public ExecutionId getExecutionId() {
        return executionId;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return String.format("TaskStartedEvent{taskId=%s, executionId=%s}", taskId, executionId);
    }
}
