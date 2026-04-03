package com.aidev.domain.repository;

import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.TaskId;
import com.aidev.domain.model.valueobject.TaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * 任务仓储接口。
 *
 * <p>领域层定义，基础设施层实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
public interface TaskRepository {

    /**
     * 根据ID查找任务。
     *
     * @param id 任务ID
     * @return 任务Optional
     */
    Optional<Task> findById(TaskId id);

    /**
     * 根据执行实例ID查找任务。
     *
     * @param executionId 执行实例ID
     * @return 任务列表
     */
    List<Task> findByExecutionId(ExecutionId executionId);

    /**
     * 根据执行实例ID和状态查找任务。
     *
     * @param executionId 执行实例ID
     * @param status 任务状态
     * @return 任务列表
     */
    List<Task> findByExecutionIdAndStatus(ExecutionId executionId, TaskStatus status);

    /**
     * 保存任务。
     *
     * @param task 任务
     * @return 保存后的任务
     */
    Task save(Task task);

    /**
     * 批量保存任务。
     *
     * @param tasks 任务列表
     * @return 保存后的任务列表
     */
    List<Task> saveAll(List<Task> tasks);
}
