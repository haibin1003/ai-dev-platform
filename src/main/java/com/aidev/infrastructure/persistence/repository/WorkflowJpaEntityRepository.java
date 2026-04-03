package com.aidev.infrastructure.persistence.repository;

import com.aidev.infrastructure.persistence.entity.WorkflowJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工作流Spring Data JPA仓储。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Repository
public interface WorkflowJpaEntityRepository extends JpaRepository<WorkflowJpaEntity, String> {

    /**
     * 根据状态查找工作流。
     *
     * @param status 状态
     * @return 工作流列表
     */
    List<WorkflowJpaEntity> findByStatus(String status);
}
