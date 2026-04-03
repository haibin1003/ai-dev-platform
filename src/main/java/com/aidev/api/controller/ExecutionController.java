package com.aidev.api.controller;

import com.aidev.api.dto.ExecutionResponse;
import com.aidev.api.dto.TaskResponse;
import com.aidev.application.service.ExecutionAppService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行控制器。
 *
 * @author AI Assistant
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {

    private final ExecutionAppService executionAppService;

    public ExecutionController(ExecutionAppService executionAppService) {
        this.executionAppService = executionAppService;
    }

    /**
     * 获取所有执行记录列表（支持筛选）。
     */
    @GetMapping
    public ResponseEntity<List<ExecutionResponse>> listExecutions(
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<ExecutionResponse> executions = executionAppService.listExecutions(
            workflowId, status, startTime, endTime);
        return ResponseEntity.ok(executions);
    }

    /**
     * 获取执行详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionResponse> getExecution(@PathVariable String id) {
        ExecutionResponse execution = executionAppService.getExecution(id);
        return ResponseEntity.ok(execution);
    }

    /**
     * 获取执行的任务列表。
     */
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> getExecutionTasks(@PathVariable String id) {
        List<TaskResponse> tasks = executionAppService.getExecutionTasks(id);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 取消执行。
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ExecutionResponse> cancelExecution(@PathVariable String id) {
        ExecutionResponse execution = executionAppService.cancelExecution(id);
        return ResponseEntity.ok(execution);
    }

    /**
     * 重试失败的任务。
     */
    @PostMapping("/{executionId}/tasks/{taskId}/retry")
    public ResponseEntity<TaskResponse> retryTask(
            @PathVariable String executionId,
            @PathVariable String taskId) {
        TaskResponse task = executionAppService.retryTask(executionId, taskId);
        return ResponseEntity.ok(task);
    }
}
