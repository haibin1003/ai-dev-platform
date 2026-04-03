package com.aidev.infrastructure.persistence.repository;

import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;
import com.aidev.domain.repository.WorkflowRepository;
import com.aidev.infrastructure.persistence.entity.WorkflowJpaEntity;
import com.aidev.infrastructure.persistence.mapper.WorkflowMapper;
import org.springframework.stereotype.Repository;

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
            .map(mapper::toDomain);
    }

    @Override
    public List<Workflow> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Workflow> findByStatus(WorkflowStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Workflow save(Workflow workflow) {
        WorkflowJpaEntity entity = mapper.toEntity(workflow);
        WorkflowJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(WorkflowId id) {
        jpaRepository.deleteById(id.getValue());
    }
}
