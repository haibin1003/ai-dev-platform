package com.aidev.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 任务执行结果（值对象）。
 *
 * <p>不可变，封装执行输出和产物。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class ExecutionResult {

    private final int exitCode;
    private final String output;
    private final String errorOutput;
    private final long durationMs;
    private final List<String> artifacts;
    private final LocalDateTime completedAt;

    private ExecutionResult(Builder builder) {
        this.exitCode = builder.exitCode;
        this.output = builder.output != null ? builder.output : "";
        this.errorOutput = builder.errorOutput != null ? builder.errorOutput : "";
        this.durationMs = builder.durationMs;
        this.artifacts = builder.artifacts != null
            ? Collections.unmodifiableList(builder.artifacts)
            : Collections.emptyList();
        this.completedAt = LocalDateTime.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorOutput() {
        return errorOutput;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public List<String> getArtifacts() {
        return artifacts;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionResult)) return false;
        ExecutionResult that = (ExecutionResult) o;
        return exitCode == that.exitCode &&
               durationMs == that.durationMs &&
               Objects.equals(output, that.output) &&
               Objects.equals(errorOutput, that.errorOutput) &&
               Objects.equals(artifacts, that.artifacts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exitCode, output, errorOutput, durationMs, artifacts);
    }

    @Override
    public String toString() {
        return String.format("ExecutionResult{exitCode=%d, durationMs=%d, success=%s}",
            exitCode, durationMs, isSuccess());
    }

    public static class Builder {
        private int exitCode;
        private String output;
        private String errorOutput;
        private long durationMs;
        private List<String> artifacts;

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        public Builder errorOutput(String errorOutput) {
            this.errorOutput = errorOutput;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder artifacts(List<String> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public ExecutionResult build() {
            return new ExecutionResult(this);
        }
    }
}
