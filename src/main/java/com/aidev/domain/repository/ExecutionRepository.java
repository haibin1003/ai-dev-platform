package com.aidev.domain.repository;

import com.aidev.domain.model.aggregate.Execution;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.ExecutionStatus;
import com.aidev.domain.model.valueobject.WorkflowId;

import java.util.List;
import java.util.Optional;

/**
 * 执行实例仓储接口。
 *
 * <p>领域层定义，基础设施层实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
public interface ExecutionRepository {

    /**
     * 根据ID查找执行实例。
     *
     * @param id 执行实例ID
     * @return 执行实例Optional
     */
    Optional<Execution> findById(ExecutionId id);

    /**
     * 根据工作流ID查找执行实例。
     *
     * @param workflowId 工作流ID
     * @return 执行实例列表
     */
    List<Execution> findByWorkflowId(WorkflowId workflowId);

    /**
     * 根据状态查找执行实例。
     *
     * @param status 执行状态
     * @return 执行实例列表
     */
    List<Execution> findByStatus(ExecutionStatus status);

    /**
     * 保存执行实例。
     *
     * @param execution 执行实例
     * @return 保存后的执行实例
     */
    Execution save(Execution execution);
}
