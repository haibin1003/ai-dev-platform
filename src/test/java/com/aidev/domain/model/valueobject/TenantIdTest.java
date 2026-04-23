package com.aidev.domain.model.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TenantId 值对象单元测试。
 *
 * @author AI Assistant
 * @since 1.0
 */
class TenantIdTest {

    @Test
    void shouldCreateFromValidString() {
        TenantId tenantId = TenantId.of("tenant-123");
        assertEquals("tenant-123", tenantId.getValue());
    }

    @Test
    void shouldGenerateRandomTenantId() {
        TenantId tenantId = TenantId.generate();
        assertNotNull(tenantId.getValue());
        assertEquals(36, tenantId.getValue().length()); // UUID format
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(NullPointerException.class, () -> TenantId.of(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> TenantId.of("  "));
    }

    @Test
    void shouldBeEqualForSameValue() {
        TenantId t1 = TenantId.of("tenant-abc");
        TenantId t2 = TenantId.of("tenant-abc");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        TenantId t1 = TenantId.of("tenant-a");
        TenantId t2 = TenantId.of("tenant-b");
        assertNotEquals(t1, t2);
    }
}
