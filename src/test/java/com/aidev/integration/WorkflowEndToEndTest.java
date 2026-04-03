package com.aidev.integration;

import com.aidev.api.dto.CreateWorkflowRequest;
import com.aidev.api.dto.ExecutionResponse;
import com.aidev.api.dto.WorkflowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 工作流端到端集成测试。
 *
 * <p>测试完整的工作流生命周期：创建 -> 激活 -> 执行 -> 查询
 *
 * @author AI Assistant
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("工作流端到端测试")
class WorkflowEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("完整工作流生命周期测试")
    void shouldCompleteFullWorkflowLifecycle() throws Exception {
        // Step 1: 创建工作流
        String workflowId = createWorkflow();
        assertThat(workflowId).isNotNull();

        // Step 2: 验证工作流创建成功
        mockMvc.perform(get("/api/v1/workflows/{id}", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT"));

        // Step 3: 激活工作流
        mockMvc.perform(post("/api/v1/workflows/{id}/activate", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Step 4: 执行工作流
        String executionId = executeWorkflow(workflowId);
        assertThat(executionId).isNotNull();

        // Step 5: 验证执行记录存在
        mockMvc.perform(get("/api/v1/executions/{id}", executionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executionId").value(executionId));

        // Step 6: 获取工作流的执行列表
        mockMvc.perform(get("/api/v1/workflows/{id}/executions", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));

        // Step 7: 获取执行的任务列表
        mockMvc.perform(get("/api/v1/executions/{id}/tasks", executionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("线性工作流执行测试")
    void shouldExecuteLinearWorkflow() throws Exception {
        // 创建线性工作流: A -> B -> C
        String workflowId = createLinearWorkflow();

        // 激活
        mockMvc.perform(post("/api/v1/workflows/{id}/activate", workflowId))
            .andExpect(status().isOk());

        // 执行
        String executionId = executeWorkflow(workflowId);

        // 验证执行创建成功
        assertThat(executionId).isNotNull();
    }

    @Test
    @DisplayName("并行工作流执行测试")
    void shouldExecuteParallelWorkflow() throws Exception {
        // 创建并行工作流: A -> [B, C] -> D
        String workflowId = createParallelWorkflow();

        // 激活
        mockMvc.perform(post("/api/v1/workflows/{id}/activate", workflowId))
            .andExpect(status().isOk());

        // 执行
        String executionId = executeWorkflow(workflowId);

        // 验证执行创建成功
        assertThat(executionId).isNotNull();
    }

    // Helper methods
    private String createWorkflow() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
            "E2E Test Workflow",
            "End to end test workflow",
            new CreateWorkflowRequest.WorkflowDefinitionDTO(
                java.util.List.of(
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "node1", "Start Node", "TASK", "claude-code", null)
                ),
                java.util.List.of()
            ),
            java.util.Map.of("key", "value")
        );

        MvcResult result = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        WorkflowResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            WorkflowResponse.class
        );
        return response.id();
    }

    private String createLinearWorkflow() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
            "Linear Workflow",
            "A -> B -> C",
            new CreateWorkflowRequest.WorkflowDefinitionDTO(
                java.util.List.of(
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "A", "Node A", "TASK", "claude-code", null),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "B", "Node B", "TASK", "claude-code", null),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "C", "Node C", "TASK", "claude-code", null)
                ),
                java.util.List.of(
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO("A", "B"),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO("B", "C")
                )
            ),
            null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createParallelWorkflow() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
            "Parallel Workflow",
            "A -> [B, C] -> D",
            new CreateWorkflowRequest.WorkflowDefinitionDTO(
                java.util.List.of(
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "A", "Node A", "TASK", "claude-code", null),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "B", "Node B", "TASK", "claude-code", null),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "C", "Node C", "TASK", "claude-code", null),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO(
                        "D", "Node D", "TASK", "claude-code", null)
                ),
                java.util.List.of(
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO("A", "B"),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO("A", "C"),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO("B", "D"),
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.EdgeDTO("C", "D")
                )
            ),
            null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String executeWorkflow(String workflowId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/workflows/{id}/execute", workflowId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isAccepted())
            .andReturn();

        ExecutionResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            ExecutionResponse.class
        );
        return response.executionId();
    }
}
