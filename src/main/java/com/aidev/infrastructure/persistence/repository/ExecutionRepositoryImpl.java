package com.aidev.infrastructure.persistence.repository;

import com.aidev.domain.model.aggregate.Execution;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.ExecutionStatus;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.repository.ExecutionRepository;
import com.aidev.infrastructure.persistence.entity.ExecutionJpaEntity;
import com.aidev.infrastructure.persistence.mapper.ExecutionMapper;
import org.springframework.stereotype.Repository;

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
            .map(mapper::toDomain);
    }

    @Override
    public List<Execution> findByWorkflowId(WorkflowId workflowId) {
        return jpaRepository.findByWorkflowId(workflowId.getValue()).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Execution> findByStatus(ExecutionStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Execution save(Execution execution) {
        ExecutionJpaEntity entity = mapper.toEntity(execution);
        ExecutionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Execution> findAll() {
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}
