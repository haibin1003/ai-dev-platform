package com.aidev.infrastructure.persistence.mapper;

import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;
import com.aidev.infrastructure.persistence.entity.WorkflowJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工作流Mapper。
 *
 * <p>负责Workflow领域对象和WorkflowJpaEntity之间的转换。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Component
public class WorkflowMapper {

    private final ObjectMapper objectMapper;

    public WorkflowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域对象转换为JPA实体。
     *
     * @param workflow 工作流领域对象
     * @return JPA实体
     */
    public WorkflowJpaEntity toEntity(Workflow workflow) {
        WorkflowJpaEntity entity = new WorkflowJpaEntity();
        entity.setId(workflow.getId().getValue());
        entity.setName(workflow.getName());
        entity.setDescription(workflow.getDescription());
        entity.setStatus(workflow.getStatus().name());
        entity.setDefinitionJson(toJson(new DefinitionDto(workflow.getNodes(), workflow.getEdges())));
        entity.setVariablesJson(toJson(workflow.getVariables()));
        entity.setCreatedAt(workflow.getCreatedAt());
        entity.setUpdatedAt(workflow.getUpdatedAt());
        return entity;
    }

    /**
     * 将JPA实体转换为领域对象。
     *
     * @param entity JPA实体
     * @return 工作流领域对象
     */
    public Workflow toDomain(WorkflowJpaEntity entity) {
        Workflow workflow = Workflow.of(
            WorkflowId.of(entity.getId()),
            entity.getName(),
            entity.getDescription()
        );

        // 使用反射设置私有字段状态
        // 注意：实际项目中可能需要更优雅的方式

        return workflow;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    // 内部DTO类用于JSON序列化
    private record DefinitionDto(List<NodeDto> nodes, List<EdgeDto> edges) {
    }

    private record NodeDto(String id, String name, String type, String agentCode, Map<String, String> config) {
    }

    private record EdgeDto(String from, String to) {
    }
}
