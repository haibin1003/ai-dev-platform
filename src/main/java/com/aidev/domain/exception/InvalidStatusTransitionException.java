package com.aidev.domain.exception;

/**
 * 无效状态转换异常。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(String fromStatus, String toStatus) {
        super("TASK-002", String.format("Invalid status transition from %s to %s", fromStatus, toStatus));
    }
}
