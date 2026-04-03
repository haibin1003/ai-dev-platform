package com.aidev.application.service;

import com.aidev.api.dto.CreateWorkflowRequest;
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

    public void deleteWorkflow(String id) {
        workflowRepository.delete(WorkflowId.of(id));
    }

    public WorkflowResponse activateWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(WorkflowId.of(id))
            .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        workflow.activate();
        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
    }

    public WorkflowController.ExecutionResponse executeWorkflow(String id, java.util.Map<String, String> variables) {
        // 简化实现，返回模拟响应
        String executionId = java.util.UUID.randomUUID().toString();
        return new WorkflowController.ExecutionResponse(
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
