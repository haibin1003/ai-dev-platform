package com.aidev.api.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流响应DTO。
 *
 * @author AI Assistant
 * @since 1.0
 */
public record WorkflowResponse(
    String id,
    String name,
    String description,
    String status,
    WorkflowDefinitionDTO definition,
    Map<String, String> variables,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record WorkflowDefinitionDTO(
        java.util.List<NodeDTO> nodes,
        java.util.List<EdgeDTO> edges
    ) {
        public record NodeDTO(
            String id,
            String name,
            String type,
            String agentCode
        ) {}

        public record EdgeDTO(
            String from,
            String to
        ) {}
    }
}
