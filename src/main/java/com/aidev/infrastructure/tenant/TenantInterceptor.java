package com.aidev.infrastructure.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 租户拦截器。
 *
 * <p>从 HTTP 请求头 {@code X-Tenant-ID} 中解析租户ID，
 * 存入 {@link TenantContext}。请求处理完成后自动清理上下文。
 *
 * <p>支持路径排除配置，用于放行公共端点（如健康检查、Swagger等）。
 *
 * @author AI Assistant
 * @since 1.0
 */
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TenantInterceptor.class);

    public static final String HEADER_X_TENANT_ID = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(HEADER_X_TENANT_ID);

        if (tenantId != null && !tenantId.isBlank()) {
            if (!isValidTenantId(tenantId)) {
                logger.warn("Invalid tenant ID format: {}", tenantId);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }
            TenantContext.set(tenantId);
        } else {
            // 无租户ID = 平台级访问
            TenantContext.clear();
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    /**
     * 校验租户ID格式。
     * <p>当前支持 UUID 格式和长度为 1-64 的字母数字组合。
     *
     * @param tenantId 租户ID
     * @return true 如果格式有效
     */
    private boolean isValidTenantId(String tenantId) {
        if (tenantId.length() > 64) {
            return false;
        }
        try {
            UUID.fromString(tenantId);
            return true;
        } catch (IllegalArgumentException e) {
            // 也允许非UUID但符合字母数字下划线中划线的格式
            return tenantId.matches("^[a-zA-Z0-9_-]+$");
        }
    }
}
