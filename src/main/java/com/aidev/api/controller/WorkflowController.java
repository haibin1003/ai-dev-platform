package com.aidev.api.controller;

import com.aidev.api.dto.CreateWorkflowRequest;
import com.aidev.api.dto.ExecutionResponse;
import com.aidev.api.dto.WorkflowResponse;
import com.aidev.application.service.ExecutionAppService;
import com.aidev.application.service.WorkflowAppService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作流控制器。
 *
 * @author AI Assistant
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowAppService workflowAppService;
    private final ExecutionAppService executionAppService;

    public WorkflowController(WorkflowAppService workflowAppService,
                             ExecutionAppService executionAppService) {
        this.workflowAppService = workflowAppService;
        this.executionAppService = executionAppService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowResponse response = workflowAppService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> listWorkflows() {
        List<WorkflowResponse> workflows = workflowAppService.listWorkflows();
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflow(@PathVariable String id) {
        WorkflowResponse workflow = workflowAppService.getWorkflow(id);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable String id,
            @Valid @RequestBody CreateWorkflowRequest request) {
        WorkflowResponse response = workflowAppService.updateWorkflow(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        workflowAppService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<WorkflowResponse> activateWorkflow(@PathVariable String id) {
        WorkflowResponse workflow = workflowAppService.activateWorkflow(id);
        return ResponseEntity.ok(workflow);
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ExecutionResponse> executeWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) ExecuteWorkflowRequest request) {
        ExecutionResponse response = executionAppService.startExecution(
            id, request != null ? request.variables() : null
        );
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<ExecutionResponse>> getWorkflowExecutions(@PathVariable String id) {
        List<ExecutionResponse> executions = executionAppService.getExecutionsByWorkflow(id);
        return ResponseEntity.ok(executions);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validateWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {
        List<String> errors = workflowAppService.validateWorkflow(request);
        if (errors.isEmpty()) {
            return ResponseEntity.ok(new ValidationResponse(true, null));
        } else {
            return ResponseEntity.ok(new ValidationResponse(false, errors));
        }
    }

    public record ExecuteWorkflowRequest(java.util.Map<String, String> variables) {}

    public record ValidationResponse(boolean valid, List<String> errors) {}
}
