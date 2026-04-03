package com.aidev.domain.model.aggregate;

import com.aidev.domain.model.valueobject.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 执行实例聚合根。
 *
 * <p>负责管理一次工作流执行的完整生命周期，包含所有任务实例。
 *
 * @author AI Assistant
 * @since 1.0
 * @see Task
 */
public class Execution {

    private final ExecutionId id;
    private final WorkflowId workflowId;
    private ExecutionStatus status;
    private final Map<String, String> variables;
    private final List<Task> tasks;
    private int completedTaskCount;
    private int failedTaskCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /**
     * 私有构造函数，使用工厂方法创建。
     */
    private Execution(ExecutionId id, WorkflowId workflowId, Map<String, String> variables) {
        this.id = Objects.requireNonNull(id, "ExecutionId cannot be null");
        this.workflowId = Objects.requireNonNull(workflowId, "WorkflowId cannot be null");
        this.variables = variables != null ? new HashMap<>(variables) : new HashMap<>();
        this.status = ExecutionStatus.PENDING;
        this.tasks = new ArrayList<>();
        this.completedTaskCount = 0;
        this.failedTaskCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 创建执行实例。
     *
     * @param workflowId 工作流ID
     * @param variables 执行变量
     * @return 新的执行实例
     */
    public static Execution create(WorkflowId workflowId, Map<String, String> variables) {
        return new Execution(ExecutionId.generate(), workflowId, variables);
    }

    /**
     * 从已存在的ID创建执行实例（用于重构）。
     */
    public static Execution of(ExecutionId id, WorkflowId workflowId, Map<String, String> variables) {
        return new Execution(id, workflowId, variables);
    }

    // ==================== 核心行为 ====================

    /**
     * 开始执行。
     *
     * @throws IllegalStateException 如果状态不是 PENDING
     */
    public void start() {
        if (status != ExecutionStatus.PENDING) {
            throw new IllegalStateException("Execution must be pending to start");
        }
        this.status = ExecutionStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 添加任务到执行实例。
     *
     * @param task 要添加的任务
     */
    public void addTask(Task task) {
        Objects.requireNonNull(task, "Task cannot be null");
        if (status != ExecutionStatus.PENDING && status != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot add task to completed execution");
        }
        tasks.add(task);
    }

    /**
     * 标记任务完成。
     *
     * @param taskId 任务ID
     */
    public void taskCompleted(TaskId taskId) {
        Task task = findTaskById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED) {
            completedTaskCount++;
            checkCompletion();
        }
    }

    /**
     * 标记任务失败。
     *
     * @param taskId 任务ID
     */
    public void taskFailed(TaskId taskId) {
        Task task = findTaskById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.FAILED) {
            failedTaskCount++;
            if (!task.canRetry()) {
                fail("Task failed and cannot be retried: " + taskId);
            }
        }
    }

    /**
     * 取消执行。
     *
     * @throws IllegalStateException 如果执行已完成
     */
    public void cancel() {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel completed execution");
        }
        this.status = ExecutionStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 检查执行是否完成。
     *
     * @return true 如果所有任务都已完成
     */
    public boolean isComplete() {
        return tasks.stream().allMatch(t -> t.getStatus().isTerminal());
    }

    /**
     * 获取指定状态的任务列表。
     *
     * @param status 任务状态
     * @return 任务列表
     */
    public List<Task> getTasksByStatus(TaskStatus status) {
        return tasks.stream()
            .filter(t -> t.getStatus() == status)
            .toList();
    }

    // ==================== 私有方法 ====================

    private Optional<Task> findTaskById(TaskId taskId) {
        return tasks.stream()
            .filter(t -> t.getId().equals(taskId))
            .findFirst();
    }

    private void checkCompletion() {
        long terminalCount = tasks.stream()
            .filter(t -> t.getStatus().isTerminal())
            .count();

        if (terminalCount == tasks.size()) {
            if (failedTaskCount > 0) {
                this.status = ExecutionStatus.FAILED;
            } else {
                this.status = ExecutionStatus.COMPLETED;
            }
            this.completedAt = LocalDateTime.now();
        }
    }

    private void fail(String message) {
        this.status = ExecutionStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }

    // ==================== Getter ====================

    public ExecutionId getId() {
        return id;
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Map<String, String> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public int getCompletedTaskCount() {
        return completedTaskCount;
    }

    public int getFailedTaskCount() {
        return failedTaskCount;
    }

    public int getTotalTaskCount() {
        return tasks.size();
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Execution)) return false;
        Execution that = (Execution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Execution{id=%s, status=%s, tasks=%d/%d}",
            id, status, completedTaskCount, tasks.size());
    }
}
