package com.aidev.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 工作流引擎配置。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Configuration
@EnableAsync
public class WorkflowEngineConfig {
    // 异步执行配置在DAGScheduler中通过ExecutorService管理
}
