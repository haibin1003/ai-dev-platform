package com.aidev.infrastructure.persistence.repository;

import com.aidev.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 任务Spring Data JPA仓储。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Repository
public interface TaskJpaEntityRepository extends JpaRepository<TaskJpaEntity, String> {

    /**
     * 根据执行实例ID查找任务。
     *
     * @param executionId 执行实例ID
     * @return 任务列表
     */
    List<TaskJpaEntity> findByExecutionId(String executionId);

    /**
     * 根据执行实例ID和状态查找任务。
     *
     * @param executionId 执行实例ID
     * @param status 状态
     * @return 任务列表
     */
    List<TaskJpaEntity> findByExecutionIdAndStatus(String executionId, String status);
}
