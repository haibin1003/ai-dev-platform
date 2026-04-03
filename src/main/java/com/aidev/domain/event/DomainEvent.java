package com.aidev.domain.event;

import java.time.LocalDateTime;

/**
 * 领域事件接口。
 *
 * <p>所有领域事件必须实现此接口。
 *
 * @author AI Assistant
 * @since 1.0
 */
public interface DomainEvent {

    /**
     * 获取事件发生时间。
     *
     * @return 发生时间
     */
    LocalDateTime getOccurredAt();

    /**
     * 获取事件类型。
     *
     * @return 事件类型字符串
     */
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
}
