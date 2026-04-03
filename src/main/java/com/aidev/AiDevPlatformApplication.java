package com.aidev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * AI 研发协同平台 - 应用入口.
 *
 * <p>核心功能：
 * <ul>
 *   <li>DAG 工作流编排</li>
 *   <li>多 Agent 协作调度</li>
 *   <li>实时状态追踪</li>
 * </ul>
 *
 * @author AI Dev Team
 * @since 1.0
 */
@SpringBootApplication
@EnableAsync
public class AiDevPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDevPlatformApplication.class, args);
    }
}
