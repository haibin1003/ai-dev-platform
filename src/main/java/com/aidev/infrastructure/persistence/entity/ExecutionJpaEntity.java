package com.aidev.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 执行实例JPA实体。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Entity
@Table(name = "executions")
public class ExecutionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "workflow_id", nullable = false, length = 36)
    private String workflowId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Lob
    @Column(name = "variables_json", columnDefinition = "CLOB")
    private String variablesJson;

    @Column(name = "completed_task_count")
    private Integer completedTaskCount = 0;

    @Column(name = "failed_task_count")
    private Integer failedTaskCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVariablesJson() { return variablesJson; }
    public void setVariablesJson(String variablesJson) { this.variablesJson = variablesJson; }
    public Integer getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(Integer completedTaskCount) { this.completedTaskCount = completedTaskCount; }
    public Integer getFailedTaskCount() { return failedTaskCount; }
    public void setFailedTaskCount(Integer failedTaskCount) { this.failedTaskCount = failedTaskCount; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
