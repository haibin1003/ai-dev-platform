package com.aidev.domain.event;

import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.TaskId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 任务失败事件。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class TaskFailedEvent implements DomainEvent {

    private final TaskId taskId;
    private final ExecutionId executionId;
    private final String errorMessage;
    private final boolean retryable;
    private final LocalDateTime occurredAt;

    public TaskFailedEvent(TaskId taskId, ExecutionId executionId, String errorMessage, boolean retryable) {
        this.taskId = Objects.requireNonNull(taskId, "TaskId cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "ExecutionId cannot be null");
        this.errorMessage = Objects.requireNonNull(errorMessage, "Error message cannot be null");
        this.retryable = retryable;
        this.occurredAt = LocalDateTime.now();
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public ExecutionId getExecutionId() {
        return executionId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return String.format("TaskFailedEvent{taskId=%s, executionId=%s, retryable=%s}",
            taskId, executionId, retryable);
    }
}
