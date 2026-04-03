package com.aidev.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 创建工作流请求DTO。
 *
 * @author AI Assistant
 * @since 1.0
 */
public record CreateWorkflowRequest(
    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 255, message = "工作流名称长度不能超过255")
    String name,

    @Size(max = 1000, message = "工作流描述长度不能超过1000")
    String description,

    @NotNull(message = "工作流定义不能为空")
    @Valid
    WorkflowDefinitionDTO definition,

    Map<String, String> variables
) {
    /**
     * 工作流定义DTO。
     */
    public record WorkflowDefinitionDTO(
        @NotNull(message = "节点列表不能为空")
        @Size(min = 1, message = "至少需要定义一个节点")
        List<NodeDTO> nodes,

        List<EdgeDTO> edges
    ) {
        /**
         * 节点DTO。
         */
        public record NodeDTO(
            @NotBlank(message = "节点ID不能为空")
            String id,

            @NotBlank(message = "节点名称不能为空")
            String name,

            String type,

            String agentCode,

            Map<String, String> config
        ) {}

        /**
         * 边DTO。
         */
        public record EdgeDTO(
            @NotBlank(message = "起始节点ID不能为空")
            String from,

            @NotBlank(message = "目标节点ID不能为空")
            String to
        ) {}
    }
}
