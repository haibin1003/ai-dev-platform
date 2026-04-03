package com.aidev.infrastructure.persistence.mapper;

import com.aidev.domain.model.aggregate.Execution;
import com.aidev.domain.model.valueobject.ExecutionId;
import com.aidev.domain.model.valueobject.ExecutionStatus;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.infrastructure.persistence.entity.ExecutionJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 执行实例Mapper。
 *
 * <p>负责Execution领域对象和ExecutionJpaEntity之间的转换。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Component
public class ExecutionMapper {

    private final ObjectMapper objectMapper;

    public ExecutionMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域对象转换为JPA实体。
     *
     * @param execution 执行实例领域对象
     * @return JPA实体
     */
    public ExecutionJpaEntity toEntity(Execution execution) {
        ExecutionJpaEntity entity = new ExecutionJpaEntity();
        entity.setId(execution.getId().getValue());
        entity.setWorkflowId(execution.getWorkflowId().getValue());
        entity.setStatus(execution.getStatus().name());
        entity.setVariablesJson(toJson(execution.getVariables()));
        entity.setStartedAt(execution.getStartedAt());
        entity.setCompletedAt(execution.getCompletedAt());
        return entity;
    }

    /**
     * 将JPA实体转换为领域对象。
     *
     * @param entity JPA实体
     * @return 执行实例领域对象
     */
    public Execution toDomain(ExecutionJpaEntity entity) {
        Execution execution = Execution.of(
            ExecutionId.of(entity.getId()),
            WorkflowId.of(entity.getWorkflowId()),
            fromJson(entity.getVariablesJson(), new TypeReference<Map<String, String>>() {})
        );

        // 恢复执行状态
        restoreExecutionState(execution, entity);

        return execution;
    }

    private void restoreExecutionState(Execution execution, ExecutionJpaEntity entity) {
        // 根据存储的状态恢复执行实例
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
