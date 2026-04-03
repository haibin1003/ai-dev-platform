package com.aidev.domain.repository;

import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;

import java.util.List;
import java.util.Optional;

/**
 * 工作流仓储接口。
 *
 * <p>领域层定义，基础设施层实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
public interface WorkflowRepository {

    /**
     * 根据ID查找工作流。
     *
     * @param id 工作流ID
     * @return 工作流Optional
     */
    Optional<Workflow> findById(WorkflowId id);

    /**
     * 查找所有工作流。
     *
     * @return 工作流列表
     */
    List<Workflow> findAll();

    /**
     * 根据状态查找工作流。
     *
     * @param status 工作流状态
     * @return 工作流列表
     */
    List<Workflow> findByStatus(WorkflowStatus status);

    /**
     * 保存工作流。
     *
     * @param workflow 工作流
     * @return 保存后的工作流
     */
    Workflow save(Workflow workflow);

    /**
     * 删除工作流。
     *
     * @param id 工作流ID
     */
    void delete(WorkflowId id);
}
