package com.aidev.infrastructure.persistence.repository;

import com.aidev.domain.model.aggregate.Execution;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.ExecutionStatus;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.repository.ExecutionRepository;
import com.aidev.infrastructure.persistence.entity.ExecutionJpaEntity;
import com.aidev.infrastructure.persistence.mapper.ExecutionMapper;
import com.aidev.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Repository;
import java.util.function.Predicate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 执行实例仓储实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Repository
public class ExecutionRepositoryImpl implements ExecutionRepository {

    private final ExecutionJpaEntityRepository jpaRepository;
    private final ExecutionMapper mapper;

    public ExecutionRepositoryImpl(ExecutionJpaEntityRepository jpaRepository, ExecutionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Execution> findById(ExecutionId id) {
        return jpaRepository.findById(id.getValue())
            .filter(tenantFilter())
            .map(mapper::toDomain);
    }

    @Override
    public List<Execution> findByWorkflowId(WorkflowId workflowId) {
        return jpaRepository.findByWorkflowId(workflowId.getValue()).stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Execution> findByStatus(ExecutionStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Execution save(Execution execution) {
        ExecutionJpaEntity entity = mapper.toEntity(execution);
        String tenantId = TenantContext.current();
        if (tenantId != null) {
            entity.setTenantId(tenantId);
        }
        ExecutionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Execution> findAll() {
        return jpaRepository.findAll().stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Execution> findByConditions(
            WorkflowId workflowId,
            ExecutionStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        String workflowIdStr = workflowId != null ? workflowId.getValue() : null;
        String statusStr = status != null ? status.name() : null;

        return jpaRepository.findByConditions(workflowIdStr, statusStr, startTime, endTime).stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    private Predicate<ExecutionJpaEntity> tenantFilter() {
        String tenantId = TenantContext.current();
        if (tenantId == null) {
            return e -> true;
        }
        return e -> tenantId.equals(e.getTenantId());
    }
}
