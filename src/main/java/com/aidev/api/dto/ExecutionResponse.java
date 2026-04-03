package com.aidev.api.dto;

import com.aidev.domain.model.valueobject.ExecutionResult;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 执行响应DTO。
 *
 * @author AI Assistant
 * @since 1.0
 */
public record ExecutionResponse(
    String executionId,
    String status,
    String statusUrl,
    String workflowId,
    Map<String, String> variables,
    int completedTaskCount,
    int totalTaskCount,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
    /**
     * 简化的构造函数（用于向后兼容）。
     */
    public ExecutionResponse(String executionId, String status, String statusUrl) {
        this(executionId, status, statusUrl, null, null, 0, 0, null, null);
    }
}
