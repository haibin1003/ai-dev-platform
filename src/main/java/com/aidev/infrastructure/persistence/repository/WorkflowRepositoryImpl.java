package com.aidev.infrastructure.persistence.repository;

import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;
import com.aidev.domain.repository.WorkflowRepository;
import com.aidev.infrastructure.persistence.entity.WorkflowJpaEntity;
import com.aidev.infrastructure.persistence.mapper.WorkflowMapper;
import com.aidev.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Repository;
import java.util.function.Predicate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工作流仓储实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Repository
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final WorkflowJpaEntityRepository jpaRepository;
    private final WorkflowMapper mapper;

    public WorkflowRepositoryImpl(WorkflowJpaEntityRepository jpaRepository, WorkflowMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Workflow> findById(WorkflowId id) {
        return jpaRepository.findById(id.getValue())
            .filter(tenantFilter())
            .map(mapper::toDomain);
    }

    @Override
    public List<Workflow> findAll() {
        return jpaRepository.findAll().stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Workflow> findByStatus(WorkflowStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Workflow save(Workflow workflow) {
        WorkflowJpaEntity entity = mapper.toEntity(workflow);
        String tenantId = TenantContext.current();
        if (tenantId != null) {
            entity.setTenantId(tenantId);
        }
        WorkflowJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(WorkflowId id) {
        jpaRepository.findById(id.getValue())
            .filter(tenantFilter())
            .ifPresent(jpaRepository::delete);
    }

    private Predicate<WorkflowJpaEntity> tenantFilter() {
        String tenantId = TenantContext.current();
        if (tenantId == null) {
            // 平台级访问：可见所有数据
            return e -> true;
        }
        return e -> tenantId.equals(e.getTenantId());
    }
}
