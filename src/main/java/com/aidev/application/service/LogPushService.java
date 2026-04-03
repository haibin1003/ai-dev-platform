package com.aidev.application.service;

import com.aidev.domain.model.valueobject.TaskId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志推送服务。
 *
 * <p>负责通过 WebSocket 向客户端推送任务执行日志。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Service
public class LogPushService {

    private static final Logger logger = LoggerFactory.getLogger(LogPushService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final SimpMessagingTemplate messagingTemplate;

    public LogPushService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 推送任务日志。
     *
     * @param executionId 执行ID
     * @param taskId 任务ID
     * @param level 日志级别（INFO, WARN, ERROR）
     * @param message 日志消息
     */
    public void pushTaskLog(String executionId, TaskId taskId, String level, String message) {
        LogMessage logMessage = new LogMessage(
            LocalDateTime.now().format(TIME_FORMATTER),
            executionId,
            taskId != null ? taskId.getValue() : null,
            level,
            message
        );

        // 推送到特定执行通道
        messagingTemplate.convertAndSend("/topic/executions/" + executionId + "/logs", logMessage);

        // 如果有任务ID，也推送到任务通道
        if (taskId != null) {
            messagingTemplate.convertAndSend(
                "/topic/executions/" + executionId + "/tasks/" + taskId.getValue() + "/logs",
                logMessage
            );
        }

        logger.debug("Pushed log for execution {}: {}", executionId, message);
    }

    /**
     * 推送执行状态变更。
     *
     * @param executionId 执行ID
     * @param status 新状态
     */
    public void pushExecutionStatus(String executionId, String status) {
        StatusMessage statusMessage = new StatusMessage(
            LocalDateTime.now().format(TIME_FORMATTER),
            executionId,
            status
        );

        messagingTemplate.convertAndSend(
            "/topic/executions/" + executionId + "/status",
            statusMessage
        );

        logger.debug("Pushed status for execution {}: {}", executionId, status);
    }

    /**
     * 推送任务状态变更。
     *
     * @param executionId 执行ID
     * @param taskId 任务ID
     * @param status 新状态
     */
    public void pushTaskStatus(String executionId, TaskId taskId, String status) {
        TaskStatusMessage taskStatusMessage = new TaskStatusMessage(
            LocalDateTime.now().format(TIME_FORMATTER),
            executionId,
            taskId.getValue(),
            status
        );

        messagingTemplate.convertAndSend(
            "/topic/executions/" + executionId + "/tasks/" + taskId.getValue() + "/status",
            taskStatusMessage
        );
    }

    /**
     * 日志消息。
     */
    public record LogMessage(
        String timestamp,
        String executionId,
        String taskId,
        String level,
        String message
    ) {}

    /**
     * 执行状态消息。
     */
    public record StatusMessage(
        String timestamp,
        String executionId,
        String status
    ) {}

    /**
     * 任务状态消息。
     */
    public record TaskStatusMessage(
        String timestamp,
        String executionId,
        String taskId,
        String status
    ) {}
}
