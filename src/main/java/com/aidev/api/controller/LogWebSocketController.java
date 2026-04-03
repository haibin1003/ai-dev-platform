package com.aidev.api.controller;

import com.aidev.application.service.LogPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

/**
 * 日志 WebSocket 控制器。
 *
 * <p>处理客户端的 WebSocket 连接和订阅请求。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Controller
public class LogWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(LogWebSocketController.class);

    /**
     * 处理客户端订阅执行日志。
     */
    @SubscribeMapping("/topic/executions/{executionId}/logs")
    public void subscribeExecutionLogs(@DestinationVariable String executionId) {
        logger.info("Client subscribed to logs for execution: {}", executionId);
    }

    /**
     * 处理客户端订阅执行状态。
     */
    @SubscribeMapping("/topic/executions/{executionId}/status")
    public void subscribeExecutionStatus(@DestinationVariable String executionId) {
        logger.info("Client subscribed to status for execution: {}", executionId);
    }

    /**
     * 处理客户端订阅任务日志。
     */
    @SubscribeMapping("/topic/executions/{executionId}/tasks/{taskId}/logs")
    public void subscribeTaskLogs(
            @DestinationVariable String executionId,
            @DestinationVariable String taskId) {
        logger.info("Client subscribed to logs for task: {} in execution: {}", taskId, executionId);
    }

    /**
     * 处理心跳消息。
     */
    @MessageMapping("/heartbeat")
    @SendTo("/topic/heartbeat")
    public HeartbeatResponse heartbeat() {
        return new HeartbeatResponse(System.currentTimeMillis(), "pong");
    }

    /**
     * 心跳响应。
     */
    public record HeartbeatResponse(long timestamp, String message) {}
}
