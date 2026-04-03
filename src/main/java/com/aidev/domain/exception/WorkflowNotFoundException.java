package com.aidev.domain.exception;

/**
 * 工作流不存在异常。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class WorkflowNotFoundException extends DomainException {

    public WorkflowNotFoundException(String workflowId) {
        super("WF-001", "Workflow not found: " + workflowId);
    }
}
