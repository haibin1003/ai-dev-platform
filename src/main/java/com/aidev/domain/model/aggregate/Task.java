package com.aidev.domain.model.aggregate;

import com.aidev.domain.event.DomainEvent;
import com.aidev.domain.event.TaskCompletedEvent;
import com.aidev.domain.event.TaskFailedEvent;
import com.aidev.domain.event.TaskStartedEvent;
import com.aidev.domain.exception.InvalidStatusTransitionException;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 任务聚合根。
 *
 * <p>负责管理任务的执行生命周期，封装状态机转换逻辑。
 *
 * @author AI Assistant
 * @since 1.0
 * @see TaskStatus
 */
public class Task {

    private final TaskId id;
    private final ExecutionId executionId;
    private final WorkflowId workflowId;
    private final NodeId nodeId;
    private TaskStatus status;
    private final Map<String, String> inputs;
    private ExecutionResult result;
    private String errorMessage;
    private int retryCount;
    private final int maxRetries;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private final List<DomainEvent> domainEvents;

    /**
     * 私有构造函数，使用工厂方法创建。
     */
    private Task(TaskId id, ExecutionId executionId, WorkflowId workflowId,
                 NodeId nodeId, Map<String, String> inputs, int maxRetries) {
        this.id = Objects.requireNonNull(id, "TaskId cannot be null");
        this.executionId = Objects.requireNonNull(executionId, "ExecutionId cannot be null");
        this.workflowId = Objects.requireNonNull(workflowId, "WorkflowId cannot be null");
        this.nodeId = Objects.requireNonNull(nodeId, "NodeId cannot be null");
        this.inputs = inputs != null ? new HashMap<>(inputs) : new HashMap<>();
        this.status = TaskStatus.PENDING;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryCount = 0;
        this.domainEvents = new ArrayList<>();
    }

    /**
     * 创建任务。
     *
     * @param executionId 执行实例ID
     * @param workflowId 工作流ID
     * @param nodeId 节点ID
     * @param inputs 输入参数
     * @param maxRetries 最大重试次数
     * @return 新的任务实例
     */
    public static Task create(ExecutionId executionId, WorkflowId workflowId,
                              NodeId nodeId, Map<String, String> inputs, int maxRetries) {
        return new Task(TaskId.generate(), executionId, workflowId, nodeId, inputs, maxRetries);
    }

    /**
     * 从已存在的ID创建任务（用于重构）。
     */
    public static Task of(TaskId id, ExecutionId executionId, WorkflowId workflowId,
                          NodeId nodeId, Map<String, String> inputs, int maxRetries) {
        return new Task(id, executionId, workflowId, nodeId, inputs, maxRetries);
    }

    // ==================== 核心行为 ====================

    /**
     * 标记任务为就绪状态（依赖已满足）。
     *
     * @throws InvalidStatusTransitionException 如果状态转换不合法
     */
    public void markReady() {
        validateTransitionTo(TaskStatus.READY);
        this.status = TaskStatus.READY;
    }

    /**
     * 开始执行任务。
     *
     * @throws InvalidStatusTransitionException 如果状态不是 READY
     */
    public void start() {
        validateTransitionTo(TaskStatus.RUNNING);
        this.status = TaskStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        recordEvent(new TaskStartedEvent(id, executionId));
    }

    /**
     * 完成任务。
     *
     * @param result 执行结果
     * @throws InvalidStatusTransitionException 如果状态不是 RUNNING
     * @throws IllegalArgumentException 如果结果为空
     */
    public void complete(ExecutionResult result) {
        Objects.requireNonNull(result, "ExecutionResult cannot be null");
        validateTransitionTo(TaskStatus.COMPLETED);

        this.status = TaskStatus.COMPLETED;
        this.result = result;
        this.completedAt = LocalDateTime.now();
        recordEvent(new TaskCompletedEvent(id, executionId, result));
    }

    /**
     * 标记任务失败。
     *
     * @param errorMessage 错误信息
     * @throws InvalidStatusTransitionException 如果状态不是 RUNNING
     */
    public void fail(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        validateTransitionTo(TaskStatus.FAILED);

        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();

        boolean canRetry = canRetry();
        recordEvent(new TaskFailedEvent(id, executionId, errorMessage, canRetry));
    }

    /**
     * 取消任务。
     *
     * @throws InvalidStatusTransitionException 如果状态不允许取消
     */
    public void cancel() {
        validateTransitionTo(TaskStatus.CANCELLED);
        this.status = TaskStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 检查是否可以重试。
     *
     * @return true 如果失败且重试次数未达上限
     */
    public boolean canRetry() {
        return status == TaskStatus.FAILED && retryCount < maxRetries;
    }

    /**
     * 增加重试计数。
     *
     * @return true 如果重试成功
     * @throws IllegalStateException 如果不能重试
     */
    public boolean retry() {
        if (!canRetry()) {
            throw new IllegalStateException("Task cannot be retried");
        }
        this.retryCount++;
        this.status = TaskStatus.READY;
        this.errorMessage = null;
        this.startedAt = null;
        this.completedAt = null;
        return true;
    }

    // ==================== 私有方法 ====================

    private void validateTransitionTo(TaskStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(status.name(), newStatus.name());
        }
    }

    private void recordEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    /**
     * 获取并清空领域事件。
     *
     * @return 事件列表
     */
    public List<DomainEvent> drainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    // ==================== Getter ====================

    public TaskId getId() {
        return id;
    }

    public ExecutionId getExecutionId() {
        return executionId;
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Map<String, String> getInputs() {
        return Collections.unmodifiableMap(inputs);
    }

    public ExecutionResult getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Task{id=%s, status=%s, nodeId=%s}", id, status, nodeId);
    }
}
