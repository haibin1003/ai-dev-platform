package com.aidev.domain.service;

import com.aidev.domain.model.valueobject.TaskStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 任务状态机（领域服务）。
 *
 * <p>集中管理任务状态转换规则，验证状态转换的合法性。
 *
 * @author AI Assistant
 * @since 1.0
 * @see com.aidev.domain.model.aggregate.Task
 */
public class TaskStateMachine {

    // 状态转换规则表
    private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS;

    static {
        Map<TaskStatus, Set<TaskStatus>> transitions = new HashMap<>();
        transitions.put(TaskStatus.PENDING, Set.of(TaskStatus.READY));
        transitions.put(TaskStatus.READY, Set.of(TaskStatus.RUNNING));
        transitions.put(TaskStatus.RUNNING, Set.of(TaskStatus.COMPLETED, TaskStatus.FAILED, TaskStatus.CANCELLED));
        transitions.put(TaskStatus.COMPLETED, Collections.emptySet());
        transitions.put(TaskStatus.FAILED, Set.of(TaskStatus.READY));  // 允许重试
        transitions.put(TaskStatus.CANCELLED, Collections.emptySet());
        VALID_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * 检查是否可以转换到目标状态。
     *
     * @param current 当前状态
     * @param target 目标状态
     * @return true 如果可以转换
     */
    public static boolean canTransition(TaskStatus current, TaskStatus target) {
        if (current == null || target == null) {
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet()).contains(target);
    }

    /**
     * 验证状态转换是否合法。
     *
     * @param current 当前状态
     * @param target 目标状态
     * @throws IllegalStateException 如果转换不合法
     */
    public static void validateTransition(TaskStatus current, TaskStatus target) {
        if (!canTransition(current, target)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", current, target)
            );
        }
    }

    /**
     * 获取从当前状态可以转换到的所有状态。
     *
     * @param current 当前状态
     * @return 可转换状态集合
     */
    public static Set<TaskStatus> getAllowedTransitions(TaskStatus current) {
        return VALID_TRANSITIONS.getOrDefault(current, Collections.emptySet());
    }

    /**
     * 检查状态是否为终态。
     *
     * @param status 状态
     * @return true 如果是终态（COMPLETED 或 CANCELLED）
     */
    public static boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED;
    }

    /**
     * 检查状态是否允许重试。
     *
     * @param status 状态
     * @return true 如果允许重试
     */
    public static boolean isRetryable(TaskStatus status) {
        return status == TaskStatus.FAILED;
    }

    /**
     * 获取初始状态。
     *
     * @return PENDING
     */
    public static TaskStatus getInitialState() {
        return TaskStatus.PENDING;
    }
}
