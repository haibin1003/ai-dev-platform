package com.aidev.infrastructure.persistence.repository;

import com.aidev.infrastructure.persistence.entity.ExecutionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * 根据条件查询执行实例。
     *
     * @param workflowId 工作流ID（可选）
     * @param status 状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 执行实例列表
     */
    @Query("SELECT e FROM ExecutionJpaEntity e WHERE " +
           "(:workflowId IS NULL OR e.workflowId = :workflowId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:startTime IS NULL OR e.startedAt >= :startTime) AND " +
           "(:endTime IS NULL OR e.startedAt <= :endTime) " +
           "ORDER BY e.startedAt DESC")
    List<ExecutionJpaEntity> findByConditions(
        @Param("workflowId") String workflowId,
        @Param("status") String status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
}
