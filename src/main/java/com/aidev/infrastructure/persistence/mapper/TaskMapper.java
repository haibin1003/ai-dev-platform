package com.aidev.infrastructure.persistence.mapper;

import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.*;
import com.aidev.infrastructure.persistence.entity.TaskJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 任务Mapper。
 *
 * <p>负责Task领域对象和TaskJpaEntity之间的转换。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Component
public class TaskMapper {

    private final ObjectMapper objectMapper;

    public TaskMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域对象转换为JPA实体。
     *
     * @param task 任务领域对象
     * @return JPA实体
     */
    public TaskJpaEntity toEntity(Task task) {
        TaskJpaEntity entity = new TaskJpaEntity();
        entity.setId(task.getId().getValue());
        entity.setExecutionId(task.getExecutionId().getValue());
        entity.setNodeId(task.getNodeId().getValue());
        entity.setStatus(task.getStatus().name());
        entity.setInputsJson(toJson(task.getInputs()));
        entity.setResultJson(toJson(task.getResult()));
        entity.setErrorMessage(task.getErrorMessage());
        entity.setRetryCount(task.getRetryCount());
        entity.setMaxRetries(task.getMaxRetries());
        entity.setStartedAt(task.getStartedAt());
        entity.setCompletedAt(task.getCompletedAt());
        return entity;
    }

    /**
     * 将JPA实体转换为领域对象。
     *
     * @param entity JPA实体
     * @return 任务领域对象
     */
    public Task toDomain(TaskJpaEntity entity) {
        Task task = Task.of(
            TaskId.of(entity.getId()),
            ExecutionId.of(entity.getExecutionId()),
            WorkflowId.generate(), // 需要改进：从执行实例获取
            NodeId.of(entity.getNodeId()),
            fromJson(entity.getInputsJson(), new TypeReference<Map<String, String>>() {}),
            entity.getMaxRetries() != null ? entity.getMaxRetries() : 3
        );

        // 恢复任务状态
        restoreTaskState(task, entity);

        return task;
    }

    private void restoreTaskState(Task task, TaskJpaEntity entity) {
        // 根据存储的状态恢复任务
        // 注意：实际实现可能需要更复杂的重构逻辑
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }
}
