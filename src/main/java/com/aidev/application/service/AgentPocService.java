package com.aidev.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent PoC 服务.
 *
 * <p>负责启动 Agent 子进程，捕获输出并推送到 SSE.
 *
 * @author AI Assistant
 * @since 1.0
 */
@Service
public class AgentPocService {

    private static final Logger logger = LoggerFactory.getLogger(AgentPocService.class);
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 运行 Agent 并实时推送输出到 SSE.
     *
     * @param agentType Agent 类型
     * @param prompt    提示词
     * @param workspace 工作目录
     * @param emitter   SSE 发射器
     */
    public void runAgent(String agentType, String prompt, String workspace, SseEmitter emitter) {
        executor.execute(() -> {
            Process process = null;
            try {
                ProcessBuilder pb = buildProcess(agentType, prompt, workspace);
                pb.redirectErrorStream(true); // 合并 stderr 到 stdout

                process = pb.start();
                logger.info("PoC: Agent process started, pid={}", process.pid());

                // 发送启动事件
                emitter.send(SseEmitter.event()
                    .name("start")
                    .data("Agent started: " + agentType));

                // 读取输出并实时推送
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("PoC output: {}", line);
                        emitter.send(SseEmitter.event()
                            .name("output")
                            .data(line));
                    }
                }

                // 等待进程结束
                int exitCode = process.waitFor();
                logger.info("PoC: Agent process exited with code {}", exitCode);

                emitter.send(SseEmitter.event()
                    .name("end")
                    .data("Agent finished with exit code: " + exitCode));

                emitter.complete();

            } catch (Exception e) {
                logger.error("PoC: Agent execution failed", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("Error: " + e.getMessage()));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    logger.error("PoC: Failed to send error event", ex);
                }
            } finally {
                if (process != null && process.isAlive()) {
                    process.destroyForcibly();
                }
            }
        });
    }

    /**
     * 根据 Agent 类型构建进程命令.
     */
    private ProcessBuilder buildProcess(String agentType, String prompt, String workspace) {
        Path workDir = workspace != null
            ? Paths.get(workspace)
            : Paths.get("").toAbsolutePath();

        return switch (agentType.toLowerCase()) {
            case "claude" -> buildClaudeProcess(prompt, workDir);
            case "opencode" -> buildOpenCodeProcess(prompt, workDir);
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };
    }

    private ProcessBuilder buildClaudeProcess(String prompt, Path workDir) {
        // Claude Code headless 模式
        // Windows 上 claude 是 npm 全局安装的 .cmd 脚本
        // 使用完整路径避免 Java ProcessBuilder 找不到命令
        String claudeCmd = System.getenv("CLAUDE_CMD_PATH");
        if (claudeCmd == null || claudeCmd.isBlank()) {
            String userHome = System.getProperty("user.home");
            claudeCmd = userHome + "\\AppData\\Roaming\\npm\\claude.cmd";
        }
        return new ProcessBuilder(
            claudeCmd,
            "--bare",
            "-p", prompt,
            "--allowedTools", "Read,Edit,Bash,Glob",
            "--output-format", "text",
            "--verbose"
        ).directory(workDir.toFile());
    }

    private ProcessBuilder buildOpenCodeProcess(String prompt, Path workDir) {
        // OpenCode one-shot 模式
        return new ProcessBuilder(
            "opencode",
            "run",
            "--format", "json",
            prompt
        ).directory(workDir.toFile());
    }
}
