package com.aidev.infrastructure.config;

import com.aidev.infrastructure.tenant.TenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 租户配置。
 *
 * <p>注册 {@link TenantInterceptor} 到 Spring MVC 拦截器链，
 * 自动从所有 HTTP 请求中解析租户ID。
 *
 * <p>排除公共端点（健康检查、Swagger、静态资源等），
 * 这些端点不需要租户上下文。
 *
 * @author AI Assistant
 * @since 1.0
 */
@Configuration
public class TenantConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantInterceptor())
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/health",
                "/api/actuator/**"
            );
    }
}
