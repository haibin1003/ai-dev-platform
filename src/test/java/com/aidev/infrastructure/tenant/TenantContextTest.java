package com.aidev.infrastructure.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantContext 单元测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetTenantId() {
        TenantContext.set("tenant-123");
        assertEquals("tenant-123", TenantContext.current());
    }

    @Test
    void shouldReturnNullWhenNotSet() {
        assertNull(TenantContext.current());
    }

    @Test
    void shouldReturnNullAfterClear() {
        TenantContext.set("tenant-123");
        TenantContext.clear();
        assertNull(TenantContext.current());
    }

    @Test
    void shouldDetectTenantPresence() {
        assertFalse(TenantContext.hasTenant());
        TenantContext.set("tenant-123");
        assertTrue(TenantContext.hasTenant());
    }

    @Test
    void shouldTreatNullAsPlatformAccess() {
        TenantContext.set(null);
        assertNull(TenantContext.current());
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    void shouldIsolateTenantsAcrossThreads() throws InterruptedException {
        TenantContext.set("tenant-main");

        Thread worker = new Thread(() -> {
            assertNull(TenantContext.current());
            TenantContext.set("tenant-worker");
            assertEquals("tenant-worker", TenantContext.current());
        });

        worker.start();
        worker.join();

        // Main thread should still have its own tenant
        assertEquals("tenant-main", TenantContext.current());
    }
}
