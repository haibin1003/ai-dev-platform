package com.aidev.api.controller;

import com.aidev.application.service.AgentPocService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent PoC 控制器.
 *
 * <p>验证核心链路：启动 Agent 进程 → 实时捕获输出 → SSE 推送到前端.
 *
 * @author AI Assistant
 * @since 1.0
 */
@RestController
@RequestMapping("/api/poc")
public class AgentPocController {

    private static final Logger logger = LoggerFactory.getLogger(AgentPocController.class);
    private final AgentPocService agentPocService;

    public AgentPocController(AgentPocService agentPocService) {
        this.agentPocService = agentPocService;
    }

    /**
     * 启动 Agent 并实时推送输出.
     *
     * <p>通过 SSE 流式返回 Agent 进程的 stdout/stderr.
     *
     * @param agentType Agent 类型：claude 或 opencode
     * @param prompt    发给 Agent 的提示词
     * @param workspace 工作目录（可选，默认为当前项目）
     * @return SseEmitter
     */
    @GetMapping(value = "/agent-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAgentOutput(
            @RequestParam(defaultValue = "claude") String agentType,
            @RequestParam String prompt,
            @RequestParam(required = false) String workspace) {

        logger.info("PoC: starting agent={}, prompt={}", agentType, prompt);

        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        agentPocService.runAgent(agentType, prompt, workspace, emitter);

        return emitter;
    }
}
