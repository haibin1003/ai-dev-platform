package com.aidev.application.port;

import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.valueobject.ExecutionResult;

/**
 * Agent适配器端口（接口）。
 *
 * <p>领域层定义，基础设施层实现。
 *
 * @author AI Assistant
 * @since 1.0
 */
public interface AgentAdapter {

    /**
     * 获取Agent代码。
     *
     * @return Agent代码
     */
    String getAgentCode();

    /**
     * 执行任务。
     *
     * @param task 任务
     * @return 执行结果
     */
    ExecutionResult execute(Task task);

    /**
     * 是否支持指定的Agent代码。
     *
     * @param agentCode Agent代码
     * @return true 如果支持
     */
    boolean supports(String agentCode);
}
