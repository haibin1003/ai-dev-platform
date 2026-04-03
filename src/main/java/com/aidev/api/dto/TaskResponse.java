package com.aidev.api.dto;

import com.aidev.domain.model.valueobject.ExecutionResult;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 任务响应DTO。
 *
 * @author AI Assistant
 * @since 1.0
 */
public record TaskResponse(
    String taskId,
    String nodeId,
    String status,
    Map<String, String> inputs,
    ExecutionResult result,
    String errorMessage,
    int retryCount,
    int maxRetries,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {}
