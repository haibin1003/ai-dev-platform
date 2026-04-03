package com.aidev.infrastructure.persistence.mapper;

import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.entity.NodeId;
import com.aidev.domain.model.entity.NodeType;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;
import com.aidev.infrastructure.persistence.entity.WorkflowJpaEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
        entity.setDefinitionJson(toJson(convertToDefinitionDto(workflow.getNodes(), workflow.getEdges())));
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

        // 恢复工作流状态
        restoreWorkflowState(workflow, entity);

        return workflow;
    }

    private void restoreWorkflowState(Workflow workflow, WorkflowJpaEntity entity) {
        // 从JSON恢复定义（节点和边）
        if (entity.getDefinitionJson() != null && !entity.getDefinitionJson().isEmpty()) {
            DefinitionDto definition = fromJson(entity.getDefinitionJson(), DefinitionDto.class);

            // 添加节点
            for (NodeDto nodeDto : definition.nodes()) {
                Node node = new Node(NodeId.of(nodeDto.id()), nodeDto.name(),
                    NodeType.valueOf(nodeDto.type()));
                if (nodeDto.agentCode() != null) {
                    node.withAgent(nodeDto.agentCode());
                }
                if (nodeDto.config() != null) {
                    nodeDto.config().forEach(node::withConfig);
                }
                workflow.addNode(node);
            }

            // 添加边
            for (EdgeDto edgeDto : definition.edges()) {
                workflow.connect(edgeDto.from(), edgeDto.to());
            }
        }

        // 恢复变量
        if (entity.getVariablesJson() != null && !entity.getVariablesJson().isEmpty()) {
            Map<String, String> variables = fromJson(entity.getVariablesJson(),
                new TypeReference<Map<String, String>>() {});
            variables.forEach(workflow::setVariable);
        }

        // 激活工作流（如果需要）
        if ("ACTIVE".equals(entity.getStatus())) {
            workflow.activate();
        } else if ("ARCHIVED".equals(entity.getStatus())) {
            workflow.archive();
        }
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

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    private DefinitionDto convertToDefinitionDto(List<Node> nodes, List<Edge> edges) {
        List<NodeDto> nodeDtos = nodes.stream()
            .map(n -> new NodeDto(
                n.getId().getValue(),
                n.getName(),
                n.getType().name(),
                n.getAgentCode(),
                n.getConfig()
            ))
            .toList();

        List<EdgeDto> edgeDtos = edges.stream()
            .map(e -> new EdgeDto(e.getFrom().getValue(), e.getTo().getValue()))
            .toList();

        return new DefinitionDto(nodeDtos, edgeDtos);
    }

    // 内部DTO类用于JSON序列化
    private record DefinitionDto(List<NodeDto> nodes, List<EdgeDto> edges) {
    }

    private record NodeDto(String id, String name, String type, String agentCode, Map<String, String> config) {
    }

    private record EdgeDto(String from, String to) {
    }
}
