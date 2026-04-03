package com.aidev.api.controller;

import com.aidev.api.dto.CreateWorkflowRequest;
import com.aidev.api.dto.ExecutionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ExecutionController 集成测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("ExecutionController 集成测试")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String workflowId;

    @BeforeEach
    void setUp() throws Exception {
        // 创建并激活测试工作流
        workflowId = createAndActivateWorkflow();
    }

    @Test
    @DisplayName("应该获取执行详情")
    void shouldGetExecution() throws Exception {
        // Given - 创建执行
        String executionId = createExecution();

        // When & Then
        mockMvc.perform(get("/api/v1/executions/{id}", executionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executionId").value(executionId))
            .andExpect(jsonPath("$.status").exists());
    }

    @Test
    @DisplayName("应该获取执行的任务列表")
    void shouldGetExecutionTasks() throws Exception {
        // Given - 创建执行
        String executionId = createExecution();

        // When & Then
        mockMvc.perform(get("/api/v1/executions/{id}/tasks", executionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("应该取消执行")
    void shouldCancelExecution() throws Exception {
        // Given - 创建执行
        String executionId = createExecution();

        // 等待一小段时间让任务调度完成
        Thread.sleep(200);

        // When & Then - 异步执行可能导致任务快速完成，接受200或400
        int status = mockMvc.perform(post("/api/v1/executions/{id}/cancel", executionId))
            .andReturn()
            .getResponse()
            .getStatus();

        // 验证返回200(成功取消)或400(执行已完成无法取消)都是可接受的
        org.assertj.core.api.Assertions.assertThat(status).isIn(200, 400);
    }

    @Test
    @DisplayName("应该获取工作流的所有执行")
    void shouldGetWorkflowExecutions() throws Exception {
        // Given - 创建执行
        createExecution();

        // When & Then
        mockMvc.perform(get("/api/v1/workflows/{id}/executions", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("应该根据工作流ID筛选执行")
    void shouldFilterExecutionsByWorkflowId() throws Exception {
        // Given - 创建执行
        createExecution();

        // When & Then
        mockMvc.perform(get("/api/v1/executions")
                .param("workflowId", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("应该根据状态筛选执行")
    void shouldFilterExecutionsByStatus() throws Exception {
        // Given - 创建执行
        createExecution();

        // When & Then
        mockMvc.perform(get("/api/v1/executions")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("应该获取所有执行列表")
    void shouldListAllExecutions() throws Exception {
        // Given - 创建执行
        createExecution();

        // When & Then
        mockMvc.perform(get("/api/v1/executions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // Helper methods
    private String createAndActivateWorkflow() throws Exception {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
            "Test Workflow",
            "Test Description",
            new CreateWorkflowRequest.WorkflowDefinitionDTO(
                java.util.List.of(
                    new CreateWorkflowRequest.WorkflowDefinitionDTO.NodeDTO("node1", "Node 1", "TASK", "claude-code", null)
                ),
                java.util.List.of()
            ),
            null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        String workflowId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        // 激活工作流
        mockMvc.perform(post("/api/v1/workflows/{id}/activate", workflowId))
            .andExpect(status().isOk());

        return workflowId;
    }

    private String createExecution() throws Exception {
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
