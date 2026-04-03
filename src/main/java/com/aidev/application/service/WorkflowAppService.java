package com.aidev.application.service;

import com.aidev.api.dto.CreateWorkflowRequest;
import com.aidev.api.dto.ExecutionResponse;
import com.aidev.api.dto.WorkflowResponse;
import com.aidev.domain.model.aggregate.Workflow;
import com.aidev.domain.model.entity.Edge;
import com.aidev.domain.model.entity.Node;
import com.aidev.domain.model.valueobject.WorkflowId;
import com.aidev.domain.model.valueobject.WorkflowStatus;
import com.aidev.domain.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作流应用服务。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Service
@Transactional
public class WorkflowAppService {

    private final WorkflowRepository workflowRepository;

    public WorkflowAppService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        Workflow workflow = Workflow.create(request.name(), request.description());

        // 添加节点
        if (request.definition() != null && request.definition().nodes() != null) {
            for (CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO nodeDto : request.definition().nodes()) {
                Node node = Node.createTask(nodeDto.id(), nodeDto.name());
                if (nodeDto.agentCode() != null) {
                    node.withAgent(nodeDto.agentCode());
                }
                workflow.addNode(node);
            }

            // 添加边
            if (request.definition().edges() != null) {
                for (CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO edgeDto : request.definition().edges()) {
                    workflow.connect(edgeDto.from(), edgeDto.to());
                }
            }
        }

        // 添加变量
        if (request.variables() != null) {
            request.variables().forEach(workflow::setVariable);
        }

        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listWorkflows() {
        return workflowRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(WorkflowId.of(id))
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        return toResponse(workflow);
    }

    public WorkflowResponse updateWorkflow(String id, CreateWorkflowRequest request) {
        // 简单实现：删除旧工作流，创建新的
        Workflow existing = workflowRepository.findById(WorkflowId.of(id))
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));

        // 创建新的工作流，保留ID
        Workflow workflow = Workflow.of(WorkflowId.of(id), request.name(), request.description());

        // 添加节点
        if (request.definition() != null && request.definition().nodes() != null) {
            for (CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO nodeDto : request.definition().nodes()) {
                Node node = Node.createTask(nodeDto.id(), nodeDto.name());
                if (nodeDto.agentCode() != null) {
                    node.withAgent(nodeDto.agentCode());
                }
                workflow.addNode(node);
            }

            // 添加边
            if (request.definition().edges() != null) {
                for (CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO edgeDto : request.definition().edges()) {
                    workflow.connect(edgeDto.from(), edgeDto.to());
                }
            }
        }

        // 添加变量
        if (request.variables() != null) {
            request.variables().forEach(workflow::setVariable);
        }

        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }

    public void deleteWorkflow(String id) {
        workflowRepository.delete(WorkflowId.of(id));
    }

    public List<String> validateWorkflow(CreateWorkflowRequest request) {
        List<String> errors = new java.util.ArrayList<>();

        if (request.definition() == null) {
            errors.add("工作流定义不能为空");
            return errors;
        }

        // 检查节点
        if (request.definition().nodes() == null || request.definition().nodes().isEmpty()) {
            errors.add("至少需要定义一个节点");
        } else {
            // 检查开始节点
            long startNodes = request.definition().nodes().stream()
                .filter(n -> "START".equalsIgnoreCase(n.type()))
                .count();
            if (startNodes == 0) {
                errors.add("缺少开始节点");
            } else if (startNodes > 1) {
                errors.add("只能有一个开始节点");
            }

            // 检查结束节点
            long endNodes = request.definition().nodes().stream()
                .filter(n -> "END".equalsIgnoreCase(n.type()))
                .count();
            if (endNodes == 0) {
                errors.add("缺少结束节点");
            }

            // 检查任务节点配置
            for (CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO node : request.definition().nodes()) {
                if ("TASK".equalsIgnoreCase(node.type()) && node.agentCode() == null) {
                    errors.add("任务节点 '" + node.name() + "' 未配置Agent");
                }
            }
        }

        // 检查循环依赖
        if (request.definition().edges() != null && hasCycle(request.definition())) {
            errors.add("工作流存在循环依赖");
        }

        return errors;
    }

    private boolean hasCycle(CreateWorkflowRequest.WorkflowDefinitionDTO definition) {
        if (definition.nodes() == null || definition.edges() == null) {
            return false;
        }

        java.util.Map<String, java.util.List<String>> adjList = new java.util.HashMap<>();
        for (CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO node : definition.nodes()) {
            adjList.put(node.id(), new java.util.ArrayList<>());
        }
        for (CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO edge : definition.edges()) {
            adjList.computeIfAbsent(edge.from(), k -> new java.util.ArrayList<>()).add(edge.to());
        }

        java.util.Set<String> visited = new java.util.HashSet<>();
        java.util.Set<String> recStack = new java.util.HashSet<>();

        java.util.function.Function<String, Boolean> dfs = new java.util.function.Function<>() {
            @Override
            public Boolean apply(String nodeId) {
                visited.add(nodeId);
                recStack.add(nodeId);

                for (String neighbor : adjList.getOrDefault(nodeId, java.util.Collections.emptyList())) {
                    if (!visited.contains(neighbor)) {
                        if (apply(neighbor)) return true;
                    } else if (recStack.contains(neighbor)) {
                        return true;
                    }
                }

                recStack.remove(nodeId);
                return false;
            }
        };

        for (String nodeId : adjList.keySet()) {
            if (!visited.contains(nodeId)) {
                if (dfs.apply(nodeId)) return true;
            }
        }
        return false;
    }

    public WorkflowResponse activateWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(WorkflowId.of(id))
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        workflow.activate();
        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }

    public ExecutionResponse executeWorkflow(String id, java.util.Map<String, String> variables) {
        // 简化实现，返回模拟响应
        String executionId = java.util.UUID.randomUUID().toString();
        return new ExecutionResponse(
            executionId,
            "PENDING",
            "/api/v1/executions/" + executionId
        );
    }

    private WorkflowResponse toResponse(Workflow workflow) {
        return new WorkflowResponse(
            workflow.getId().getValue(),
            workflow.getName(),
            workflow.getDescription(),
            workflow.getStatus().name(),
            new WorkflowResponse.WorkflowDefinitionDTO(
                workflow.getNodes().stream()
                    .map(n -> new WorkflowResponse.WorkflowDefinitionDTO.NodeDTO(
                        n.getId().getValue(),
                        n.getName(),
                        n.getType().name(),
                        n.getAgentCode()
                    ))
                    .collect(Collectors.toList()),
                workflow.getEdges().stream()
                    .map(e -> new WorkflowResponse.WorkflowDefinitionDTO.EdgeDTO(
                        e.getFrom().getValue(),
                        e.getTo().getValue()
                    ))
                    .collect(Collectors.toList())
            ),
            workflow.getVariables(),
            workflow.getCreatedAt(),
            workflow.getUpdatedAt()
        );
    }
}
