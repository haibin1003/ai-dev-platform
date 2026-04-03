package com.aidev.infrastructure.persistence.repository;

import com.aidev.infrastructure.persistence.entity.ExecutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 执行实例Spring Data JPA仓储。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Repository
public interface ExecutionJpaEntityRepository extends JpaRepository<ExecutionJpaEntity, String> {

    /**
     * 根据工作流ID查找执行实例。
     *
     * @param workflowId 工作流ID
     * @return 执行实例列表
     */
    List<ExecutionJpaEntity> findByWorkflowId(String workflowId);

    /**
     * 根据状态查找执行实例。
     *
     * @param status 状态
     * @return 执行实例列表
     */
    List<ExecutionJpaEntity> findByStatus(String status);
}
