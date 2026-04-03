package com.aidev.api.dto;

/**
 * 执行响应DTO。
 *
 * @author AI Assistant
 * @since 1.0
 */
public record ExecutionResponse(
    String executionId,
    String status,
    String statusUrl
) {}
