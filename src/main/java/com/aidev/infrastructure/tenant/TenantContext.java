package com.aidev.infrastructure.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户上下文。
 *
 * <p>基于 ThreadLocal 存储当前请求的租户ID，支持多线程环境下的租户隔离。
 * 每个 HTTP 请求处理线程独立持有自己的租户上下文。
 *
 * @author AI Assistant
 * @since 1.0
 */
public final class TenantContext {

    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // utility class
    }

    /**
     * 设置当前线程的租户ID。
     *
     * @param tenantId 租户ID，可为 null（表示平台级访问）
     */
    public static void set(String tenantId) {
        if (tenantId != null) {
            logger.debug("TenantContext set: {}", tenantId);
            CURRENT_TENANT.set(tenantId);
        } else {
            logger.debug("TenantContext cleared (platform access)");
            CURRENT_TENANT.remove();
        }
    }

    /**
     * 获取当前线程的租户ID。
     *
     * @return 租户ID，如果未设置则返回 null（平台级访问）
     */
    public static String current() {
        return CURRENT_TENANT.get();
    }

    /**
     * 检查当前是否有租户上下文。
     *
     * @return true 如果已设置租户ID
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * 清除当前线程的租户上下文。
     * <p>必须在请求处理完成后调用，防止线程复用时的数据泄漏。
     */
    public static void clear() {
        logger.debug("TenantContext cleared");
        CURRENT_TENANT.remove();
    }
}
