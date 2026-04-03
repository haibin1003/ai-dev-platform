package com.aidev.domain.exception;

/**
 * 领域异常基类。
 *
 * <p>所有领域层异常必须继承此类。
 *
 * @author AI Assistant
 * @since 1.0
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
