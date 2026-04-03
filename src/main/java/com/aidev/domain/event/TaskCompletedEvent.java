package com.aidev.domain.event;

import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.ExecutionResult;
import com.aidev.domain.model.valueobject.TaskId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 任务完成事件。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class TaskCompletedEvent implements DomainEvent {

    private final TaskId taskId;
    private final ExecutionId executionId;
    private final ExecutionResult result;
    private final LocalDateTime occurredAt;

    public TaskCompletedEvent(TaskId taskId, ExecutionId executionId, ExecutionResult result) {
        this.taskId = Objects.requireNonNull(taskId, "TaskId cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "ExecutionId cannot be null");
        this.result = Objects.requireNonNull(result, "Result cannot be null");
        this.occurredAt = LocalDateTime.now();
    }

    public TaskId getTaskId() {
        return taskId;
    }

    public ExecutionId getExecutionId() {
        return executionId;
    }

    public ExecutionResult getResult() {
        return result;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return String.format("TaskCompletedEvent{taskId=%s, executionId=%s, success=%s}",
            taskId, executionId, result.isSuccess());
    }
}
