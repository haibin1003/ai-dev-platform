package com.aidev.application.service;

import com.aidev.application.port.AgentAdapter;
import com.aidev.domain.event.TaskCompletedEvent;
import com.aidev.domain.event.TaskFailedEvent;
import com.aidev.domain.model.aggregate.Execution;
import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.*;
import com.aidev.domain.repository.ExecutionRepository;
import com.aidev.domain.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * DAG调度器（应用服务）。
 *
 * <p>负责工作流执行的任务调度、依赖管理和并发执行。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Service
public class DAGScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DAGScheduler.class);

    private final TaskRepository taskRepository;
    private final ExecutionRepository executionRepository;
    private final com.aidev.domain.repository.WorkflowRepository workflowRepository;
    private final List<AgentAdapter> agentAdapters;
    private final ApplicationEventPublisher eventPublisher;
    private final ExecutorService executorService;

    public DAGScheduler(TaskRepository taskRepository,
                       ExecutionRepository executionRepository,
                       com.aidev.domain.repository.WorkflowRepository workflowRepository,
                       List<AgentAdapter> agentAdapters,
                       ApplicationEventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.executionRepository = executionRepository;
        this.workflowRepository = workflowRepository;
        this.agentAdapters = agentAdapters;
        this.eventPublisher = eventPublisher;
        // 创建有界线程池用于并发任务执行
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = Math.max(4, availableProcessors * 2);
        this.executorService = Executors.newFixedThreadPool(maxPoolSize, r -> {
            Thread t = new Thread(r, "dag-scheduler-" + System.nanoTime());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 初始化执行实例的任务。
     *
     * <p>根据工作流定义创建所有任务，并设置初始依赖关系。
     *
     * @param execution 执行实例
     * @param nodes 工作流节点列表
     * @param edges 工作流边列表（依赖关系）
     */
    @Transactional
    public void initializeTasks(Execution execution, List<Node> nodes, List<Edge> edges) {
        logger.info("Initializing tasks for execution {}", execution.getId());

        // 构建节点ID到任务的映射
        Map<NodeId, Task> nodeTaskMap = new HashMap<>();

        // 为每个节点创建任务
        for (Node node : nodes) {
            Task task = Task.create(
                execution.getId(),
                execution.getWorkflowId(),
                node.getId(),
                execution.getVariables(),
                3 // 默认最大重试次数
            );
            nodeTaskMap.put(node.getId(), task);
            execution.addTask(task);
        }

        // 标记入度为0的任务为就绪状态（无依赖）
        Set<NodeId> nodesWithDependencies = edges.stream()
            .map(Edge::getTo)
            .collect(Collectors.toSet());

        for (Node node : nodes) {
            if (!nodesWithDependencies.contains(node.getId())) {
                Task task = nodeTaskMap.get(node.getId());
                task.markReady();
                logger.debug("Task {} marked as READY (no dependencies)", task.getId());
            }
        }

        // 保存所有任务
        taskRepository.saveAll(execution.getTasks());
        executionRepository.save(execution);

        logger.info("Initialized {} tasks for execution {}", nodes.size(), execution.getId());
    }

    /**
     * 调度就绪任务。
     *
     * <p>查找所有就绪状态的任务并提交执行。
     *
     * @param executionId 执行实例ID
     */
    @Async
    public void scheduleReadyTasks(ExecutionId executionId) {
        logger.debug("Scheduling ready tasks for execution {}", executionId);

        List<Task> readyTasks = taskRepository.findByExecutionIdAndStatus(executionId, TaskStatus.READY);

        if (readyTasks.isEmpty()) {
            logger.debug("No ready tasks for execution {}", executionId);
            return;
        }

        logger.info("Found {} ready tasks for execution {}", readyTasks.size(), executionId);

        // 并行提交所有就绪任务
        List<CompletableFuture<Void>> futures = readyTasks.stream()
            .map(this::submitTaskAsync)
            .toList();

        // 等待所有任务提交完成（不等待执行完成）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .exceptionally(ex -> {
                logger.error("Error scheduling tasks for execution {}", executionId, ex);
                return null;
            });
    }

    /**
     * 异步提交任务执行。
     *
     * @param task 任务
     * @return CompletableFuture
     */
    private CompletableFuture<Void> submitTaskAsync(Task task) {
        return CompletableFuture.runAsync(() -> executeTask(task), executorService);
    }

    /**
     * 执行单个任务。
     *
     * @param task 任务
     */
    public void executeTask(Task task) {
        // 重新加载任务以确保状态最新
        Task freshTask = taskRepository.findById(task.getId())
            .orElseThrow(() -> new IllegalStateException("Task not found: " + task.getId()));

        if (freshTask.getStatus() != TaskStatus.READY) {
            logger.warn("Task {} is not in READY state, current state: {}",
                freshTask.getId(), freshTask.getStatus());
            return;
        }

        // 获取工作流定义中的边（依赖关系）
        List<Edge> edges = workflowRepository.findById(freshTask.getWorkflowId())
            .map(com.aidev.domain.model.aggregate.Workflow::getEdges)
            .orElse(List.of());

        // 开始执行任务
        freshTask.start();
        taskRepository.save(freshTask);

        logger.info("Executing task {} with node {}", freshTask.getId(), freshTask.getNodeId());

        try {
            // 查找合适的Agent适配器
            AgentAdapter adapter = findAdapter(freshTask);

            // 执行任务
            ExecutionResult result = adapter.execute(freshTask);

            // 处理执行结果（在新事务中）
            handleTaskCompleted(freshTask, result, edges);

        } catch (Exception e) {
            logger.error("Task execution failed: {}", freshTask.getId(), e);
            handleTaskFailed(freshTask, e.getMessage());
        }
    }

    /**
     * 处理任务完成。
     *
     * @param task 任务
     * @param result 执行结果
     */
    @Transactional
    public void handleTaskCompleted(Task task, ExecutionResult result, List<Edge> edges) {
        logger.info("Task {} completed with exit code {}", task.getId(), result.getExitCode());

        // 完成任务
        task.complete(result);
        taskRepository.save(task);

        // 更新执行实例状态
        Execution execution = executionRepository.findById(task.getExecutionId())
            .orElseThrow(() -> new IllegalStateException("Execution not found: " + task.getExecutionId()));
        execution.taskCompleted(task.getId());
        executionRepository.save(execution);

        // 发布领域事件
        eventPublisher.publishEvent(new TaskCompletedEvent(
            task.getId(),
            task.getExecutionId(),
            result
        ));

        // 检查是否可以调度后续任务
        if (!execution.isComplete()) {
            scheduleNextTasks(execution, task, edges);
        }
    }

    /**
     * 处理任务失败。
     *
     * @param task 任务
     * @param errorMessage 错误信息
     */
    @Transactional
    public void handleTaskFailed(Task task, String errorMessage) {
        logger.error("Task {} failed: {}", task.getId(), errorMessage);

        // 标记任务失败
        task.fail(errorMessage);
        taskRepository.save(task);

        // 更新执行实例状态
        Execution execution = executionRepository.findById(task.getExecutionId())
            .orElseThrow(() -> new IllegalStateException("Execution not found: " + task.getExecutionId()));
        execution.taskFailed(task.getId());
        executionRepository.save(execution);

        // 发布领域事件
        boolean retryable = task.canRetry();
        eventPublisher.publishEvent(new TaskFailedEvent(
            task.getId(),
            task.getExecutionId(),
            errorMessage,
            retryable
        ));

        // 如果可以重试，则重试任务
        if (retryable) {
            logger.info("Retrying task {} (attempt {}/{})",
                task.getId(), task.getRetryCount(), task.getMaxRetries());
            task.retry();
            task.markReady();
            taskRepository.save(task);

            // 重新调度
            submitTaskAsync(task);
        }
    }

    /**
     * 调度后续任务。
     *
     * <p>当任务完成时，检查其下游任务是否满足执行条件。
     *
     * @param execution 执行实例
     * @param completedTask 已完成的任务
     * @param edges 工作流边列表（依赖关系）
     */
    @Transactional
    public void scheduleNextTasks(Execution execution, Task completedTask, List<Edge> edges) {
        logger.debug("Checking next tasks after completion of {}", completedTask.getId());

        // 获取执行实例中的所有任务
        List<Task> allTasks = taskRepository.findByExecutionId(execution.getId());

        // 找到处于PENDING状态且依赖已满足的任务
        for (Task task : allTasks) {
            if (task.getStatus() == TaskStatus.PENDING) {
                // 检查依赖是否满足
                if (areDependenciesMet(task, allTasks, edges)) {
                    task.markReady();
                    taskRepository.save(task);
                    logger.info("Task {} marked as READY (dependencies met)", task.getId());

                    // 异步提交执行
                    submitTaskAsync(task);
                }
            }
        }
    }

    /**
     * 检查任务的依赖是否满足。
     *
     * <p>所有上游任务必须已完成。
     *
     * @param task 待检查的任务
     * @param allTasks 执行实例中的所有任务
     * @param edges 工作流边列表（依赖关系）
     * @return true 如果依赖已满足
     */
    private boolean areDependenciesMet(Task task, List<Task> allTasks, List<Edge> edges) {
        // 构建节点ID到任务的映射
        Map<NodeId, Task> nodeTaskMap = allTasks.stream()
            .collect(Collectors.toMap(Task::getNodeId, t -> t));

        // 查找该任务对应节点的所有上游节点
        List<NodeId> upstreamNodes = edges.stream()
            .filter(edge -> edge.getTo().equals(task.getNodeId()))
            .map(Edge::getFrom)
            .toList();

        // 如果没有上游节点，依赖已满足
        if (upstreamNodes.isEmpty()) {
            return true;
        }

        // 检查所有上游任务是否已完成
        for (NodeId upstreamNodeId : upstreamNodes) {
            Task upstreamTask = nodeTaskMap.get(upstreamNodeId);
            if (upstreamTask == null) {
                logger.warn("Upstream task not found for node: {}", upstreamNodeId);
                return false;
            }
            // 上游任务必须处于终态（COMPLETED, FAILED, CANCELLED）
            if (!upstreamTask.getStatus().isTerminal()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 查找支持任务的Agent适配器。
     *
     * @param task 任务
     * @return Agent适配器
     * @throws IllegalStateException 如果找不到适配器
     */
    private AgentAdapter findAdapter(Task task) {
        // 默认使用 claude-code
        String agentCode = "claude-code";

        return agentAdapters.stream()
            .filter(adapter -> adapter.supports(agentCode))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No adapter found for agent: " + agentCode));
    }

    /**
     * 取消执行实例的所有任务。
     *
     * @param executionId 执行实例ID
     */
    @Transactional
    public void cancelExecution(ExecutionId executionId) {
        logger.info("Cancelling execution {}", executionId);

        Execution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new IllegalStateException("Execution not found: " + executionId));

        // 取消执行实例
        execution.cancel();
        executionRepository.save(execution);

        // 取消所有非终态任务
        List<Task> tasks = taskRepository.findByExecutionId(executionId);
        for (Task task : tasks) {
            if (!task.getStatus().isTerminal()) {
                task.cancel();
                taskRepository.save(task);
            }
        }

        logger.info("Execution {} cancelled", executionId);
    }
}
