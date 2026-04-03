package com.aidev.infrastructure.adapter.claudecode;

import com.aidev.application.port.AgentAdapter;
import com.aidev.domain.model.aggregate.Task;
import com.aidev.domain.model.valueobject.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Claude Code 适配器。
 *
 * <p>通过命令行调用 Claude Code CLI 执行任务。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Component
public class ClaudeCodeAdapter implements AgentAdapter {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeCodeAdapter.class);
    private static final String AGENT_CODE = "claude-code";
    private static final long DEFAULT_TIMEOUT_SECONDS = 300;

    @Value("${workflow.agents.claude-code.command:claude}")
    private String command;

    @Value("${workflow.agents.claude-code.workspace:./workspace}")
    private String workspacePath;

    @Override
    public String getAgentCode() {
        return AGENT_CODE;
    }

    @Override
    public ExecutionResult execute(Task task) {
        logger.info("Executing task {} with Claude Code", task.getId());

        try {
            // 创建任务工作目录
            Path taskWorkspace = createTaskWorkspace(task);

            // 构建命令
            List<String> commandList = buildCommand(task, taskWorkspace);

            // 执行进程
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.directory(taskWorkspace.toFile());
            processBuilder.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();
            Process process = processBuilder.start();

            // 读取输出
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 等待完成
            boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ExecutionResult.builder()
                    .exitCode(-1)
                    .output(output)
                    .errorOutput("Task execution timeout")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
            }

            int exitCode = process.exitValue();
            long durationMs = System.currentTimeMillis() - startTime;

            // 收集产物
            List<String> artifacts = collectArtifacts(taskWorkspace);

            return ExecutionResult.builder()
                .exitCode(exitCode)
                .output(output)
                .durationMs(durationMs)
                .artifacts(artifacts)
                .build();

        } catch (IOException e) {
            logger.error("Failed to execute task", e);
            return ExecutionResult.builder()
                .exitCode(-1)
                .errorOutput("Execution failed: " + e.getMessage())
                .durationMs(0)
                .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.builder()
                .exitCode(-1)
                .errorOutput("Execution interrupted")
                .durationMs(0)
                .build();
        }
    }

    @Override
    public boolean supports(String agentCode) {
        return AGENT_CODE.equals(agentCode);
    }

    // ==================== 私有方法 ====================

    private Path createTaskWorkspace(Task task) throws IOException {
        Path workspace = Path.of(workspacePath, task.getExecutionId().getValue(), task.getId().getValue());
        Files.createDirectories(workspace);
        return workspace;
    }

    private List<String> buildCommand(Task task, Path workspace) {
        // 简化实现，实际应根据任务配置构建命令
        return Arrays.asList(
            command,
            "--prompt",
            "Execute task: " + task.getNodeId().getValue()
        );
    }

    private List<String> collectArtifacts(Path workspace) {
        // 收集工作目录中的文件作为产物
        try {
            return Files.walk(workspace)
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .toList();
        } catch (IOException e) {
            logger.warn("Failed to collect artifacts", e);
            return List.of();
        }
    }
}
