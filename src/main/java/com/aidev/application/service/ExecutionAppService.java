package com.aidev.application.service;

import com.aidev.api.dto.ExecutionResponse;
import com.aidev.api.dto.TaskResponse;
import com.aidev.domain.model.aggregate.Execution;
import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.ExecutionStatus;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.repository.ExecutionRepository;
import com.aidev.domain.repository.TaskRepository;
import com.aidev.domain.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行应用服务。
 *
 * <p>负责执行生命周期管理，协调DAGScheduler完成工作流执行。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Service
@Transactional
public class ExecutionAppService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionAppService.class);

    private final ExecutionRepository executionRepository;
    private final TaskRepository taskRepository;
    private final WorkflowRepository workflowRepository;
    private final DAGScheduler dagScheduler;

    public ExecutionAppService(ExecutionRepository executionRepository,
                              TaskRepository taskRepository,
                              WorkflowRepository workflowRepository,
                              DAGScheduler dagScheduler) {
        this.executionRepository = executionRepository;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
        this.dagScheduler = dagScheduler;
    }

    /**
     * 启动工作流执行。
     *
     * @param workflowId 工作流ID
     * @param variables 执行变量
     * @return 执行响应
     */
    public ExecutionResponse startExecution(String workflowId, java.util.Map<String, String> variables) {
        logger.info("Starting execution for workflow {}", workflowId);

        // 查找工作流
        Workflow workflow = workflowRepository.findById(WorkflowId.of(workflowId))
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // 检查工作流状态
        if (!workflow.canExecute()) {
            throw new IllegalStateException("Workflow cannot be executed in status: " + workflow.getStatus());
        }

        // 创建执行实例
        Execution execution = Execution.create(workflow.getId(), variables);

        // 初始化任务
        dagScheduler.initializeTasks(execution, workflow.getNodes(), workflow.getEdges());

        // 启动执行
        execution.start();
        executionRepository.save(execution);

        logger.info("Execution {} started for workflow {}", execution.getId(), workflowId);

        // 异步调度就绪任务
        dagScheduler.scheduleReadyTasks(execution.getId());

        return toResponse(execution);
    }

    /**
     * 获取执行详情。
     *
     * @param executionId 执行ID
     * @return 执行响应
     */
    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(String executionId) {
        Execution execution = executionRepository.findById(ExecutionId.of(executionId))
            .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));
        return toResponse(execution);
    }

    /**
     * 获取执行实例的任务列表。
     *
     * @param executionId 执行ID
     * @return 任务响应列表
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getExecutionTasks(String executionId) {
        List<Task> tasks = taskRepository.findByExecutionId(ExecutionId.of(executionId));
        return tasks.stream()
            .map(this::toTaskResponse)
            .collect(Collectors.toList());
    }

    /**
     * 取消执行。
     *
     * @param executionId 执行ID
     * @return 执行响应
     */
    public ExecutionResponse cancelExecution(String executionId) {
        logger.info("Cancelling execution {}", executionId);

        ExecutionId id = ExecutionId.of(executionId);
        dagScheduler.cancelExecution(id);

        Execution execution = executionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Execution not found: " + executionId));

        return toResponse(execution);
    }

    /**
     * 获取工作流的所有执行记录。
     *
     * @param workflowId 工作流ID
     * @return 执行响应列表
     */
    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutionsByWorkflow(String workflowId) {
        List<Execution> executions = executionRepository.findByWorkflowId(WorkflowId.of(workflowId));
        return executions.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * 重试失败的任务。
     *
     * @param executionId 执行ID
     * @param taskId 任务ID
     * @return 任务响应
     */
    public TaskResponse retryTask(String executionId, String taskId) {
        logger.info("Retrying task {} in execution {}", taskId, executionId);

        Task task = taskRepository.findById(com.aidev.domain.model.valueobject.TaskId.of(taskId))
            .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if (!task.canRetry()) {
            throw new IllegalStateException("Task cannot be retried");
        }

        // 重试任务
        task.retry();
        task.markReady();
        taskRepository.save(task);

        // 调度任务执行
        dagScheduler.scheduleReadyTasks(task.getExecutionId());

        return toTaskResponse(task);
    }

    // ==================== 私有方法 ====================

    private ExecutionResponse toResponse(Execution execution) {
        return new ExecutionResponse(
            execution.getId().getValue(),
            execution.getStatus().name(),
            "/api/v1/executions/" + execution.getId().getValue(),
            execution.getWorkflowId().getValue(),
            execution.getVariables(),
            execution.getCompletedTaskCount(),
            execution.getTotalTaskCount(),
            execution.getStartedAt(),
            execution.getCompletedAt()
        );
    }

    private TaskResponse toTaskResponse(Task task) {
        return new TaskResponse(
            task.getId().getValue(),
            task.getNodeId().getValue(),
            task.getStatus().name(),
            task.getInputs(),
            task.getResult(),
            task.getErrorMessage(),
            task.getRetryCount(),
            task.getMaxRetries(),
            task.getStartedAt(),
            task.getCompletedAt()
        );
    }
}
