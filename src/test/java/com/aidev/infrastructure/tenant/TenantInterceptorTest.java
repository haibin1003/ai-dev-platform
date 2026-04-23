package com.aidev.infrastructure.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantInterceptor 单元测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
class TenantInterceptorTest {

    private TenantInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExtractTenantIdFromHeader() {
        request.addHeader(TenantInterceptor.HEADER_X_TENANT_ID, "tenant-abc-123");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals("tenant-abc-123", TenantContext.current());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowPlatformAccessWithoutHeader() {
        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertNull(TenantContext.current());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectInvalidTenantIdFormat() {
        request.addHeader(TenantInterceptor.HEADER_X_TENANT_ID, "invalid@tenant#id!");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(400, response.getStatus());
    }

    @Test
    void shouldAcceptUuidFormatTenantId() {
        request.addHeader(TenantInterceptor.HEADER_X_TENANT_ID, "550e8400-e29b-41d4-a716-446655440000");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", TenantContext.current());
    }

    @Test
    void shouldClearContextAfterCompletion() {
        request.addHeader(TenantInterceptor.HEADER_X_TENANT_ID, "tenant-123");
        interceptor.preHandle(request, response, new Object());
        assertEquals("tenant-123", TenantContext.current());

        interceptor.afterCompletion(request, response, new Object(), null);

        assertNull(TenantContext.current());
    }

    @Test
    void shouldRejectTenantIdLongerThan64Chars() {
        request.addHeader(TenantInterceptor.HEADER_X_TENANT_ID, "a".repeat(65));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(400, response.getStatus());
    }
}
