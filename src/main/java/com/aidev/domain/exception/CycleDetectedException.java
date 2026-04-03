package com.aidev.domain.exception;

/**
 * 循环依赖检测异常。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class CycleDetectedException extends DomainException {

    public CycleDetectedException() {
        super("WF-004", "Cycle detected in workflow definition");
    }

    public CycleDetectedException(String message) {
        super("WF-004", "Cycle detected: " + message);
    }
}
