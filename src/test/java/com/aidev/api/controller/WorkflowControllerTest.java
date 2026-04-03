package com.aidev.api.controller;

import com.aidev.api.dto.CreateWorkflowRequest;
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
 * WorkflowController 集成测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("WorkflowController 集成测试")
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("应该成功创建工作流")
    void shouldCreateWorkflow() throws Exception {
        // Given
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

        // When & Then
        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Test Workflow"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("应该获取工作流列表")
    void shouldListWorkflows() throws Exception {
        // Given - 先创建一个工作流
        createTestWorkflow();

        // When & Then
        mockMvc.perform(get("/api/v1/workflows"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("应该获取工作流详情")
    void shouldGetWorkflow() throws Exception {
        // Given - 创建工作流
        String workflowId = createTestWorkflow();

        // When & Then
        mockMvc.perform(get("/api/v1/workflows/{id}", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(workflowId))
            .andExpect(jsonPath("$.name").value("Test Workflow"));
    }

    @Test
    @DisplayName("应该激活工作流")
    void shouldActivateWorkflow() throws Exception {
        // Given - 创建工作流
        String workflowId = createTestWorkflow();

        // When & Then
        mockMvc.perform(post("/api/v1/workflows/{id}/activate", workflowId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("应该删除工作流")
    void shouldDeleteWorkflow() throws Exception {
        // Given - 创建工作流
        String workflowId = createTestWorkflow();

        // When & Then
        mockMvc.perform(delete("/api/v1/workflows/{id}", workflowId))
            .andExpect(status().isNoContent());

        // 验证已删除
        mockMvc.perform(get("/api/v1/workflows/{id}", workflowId))
            .andExpect(status().is5xxServerError()); // 或 404
    }

    @Test
    @DisplayName("应该执行工作流")
    void shouldExecuteWorkflow() throws Exception {
        // Given - 创建并激活工作流
        String workflowId = createTestWorkflow();
        mockMvc.perform(post("/api/v1/workflows/{id}/activate", workflowId));

        // When & Then
        mockMvc.perform(post("/api/v1/workflows/{id}/execute", workflowId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.executionId").exists())
            .andExpect(jsonPath("$.status").exists()); // 状态可能是PENDING或RUNNING（异步执行）
    }

    @Test
    @DisplayName("无效请求应该返回400")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - 无效请求（缺少必填字段）
        String invalidRequest = "{}";

        // When & Then
        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    // Helper method
    private String createTestWorkflow() throws Exception {
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

        WorkflowResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            WorkflowResponse.class
        );
        return response.id();
    }
}
