package com.aidev.infrastructure.persistence.repository;

import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.TaskId;
import com.aidev.domain.model.valueobject.TaskStatus;
import com.aidev.domain.repository.TaskRepository;
import com.aidev.infrastructure.persistence.entity.TaskJpaEntity;
import com.aidev.infrastructure.persistence.mapper.TaskMapper;
import com.aidev.infrastructure.tenant.TenantContext;
import org.springframework.stereotype.Repository;
import java.util.function.Predicate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 任务仓储实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Repository
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskJpaEntityRepository jpaRepository;
    private final TaskMapper mapper;

    public TaskRepositoryImpl(TaskJpaEntityRepository jpaRepository, TaskMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Task> findById(TaskId id) {
        return jpaRepository.findById(id.getValue())
            .filter(tenantFilter())
            .map(mapper::toDomain);
    }

    @Override
    public List<Task> findByExecutionId(ExecutionId executionId) {
        return jpaRepository.findByExecutionId(executionId.getValue()).stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByExecutionIdAndStatus(ExecutionId executionId, TaskStatus status) {
        return jpaRepository.findByExecutionIdAndStatus(executionId.getValue(), status.name()).stream()
            .filter(tenantFilter())
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Task save(Task task) {
        TaskJpaEntity entity = mapper.toEntity(task);
        String tenantId = TenantContext.current();
        if (tenantId != null) {
            entity.setTenantId(tenantId);
        }
        TaskJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Task> saveAll(List<Task> tasks) {
        String tenantId = TenantContext.current();
        List<TaskJpaEntity> entities = tasks.stream()
            .map(mapper::toEntity)
            .peek(e -> {
                if (tenantId != null) {
                    e.setTenantId(tenantId);
                }
            })
            .collect(Collectors.toList());
        List<TaskJpaEntity> saved = jpaRepository.saveAll(entities);
        return saved.stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    private Predicate<TaskJpaEntity> tenantFilter() {
        String tenantId = TenantContext.current();
        if (tenantId == null) {
            return e -> true;
        }
        return e -> tenantId.equals(e.getTenantId());
    }
}
