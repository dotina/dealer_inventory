package com.dealer.dealer_inventory.security;

import com.dealer.dealer_inventory.exception.MissingTenantException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_returnsSameValue() {
        TenantContext.set("tenant-1");
        assertEquals("tenant-1", TenantContext.get());
    }

    @Test
    void get_whenNotSet_returnsNull() {
        assertNull(TenantContext.get());
    }

    @Test
    void require_whenSet_returnsValue() {
        TenantContext.set("tenant-2");
        assertEquals("tenant-2", TenantContext.require());
    }

    @Test
    void require_whenNotSet_throwsMissingTenantException() {
        assertThrows(MissingTenantException.class, TenantContext::require);
    }

    @Test
    void clear_removesValue() {
        TenantContext.set("tenant-3");
        TenantContext.clear();
        assertNull(TenantContext.get());
    }

    @Test
    void set_overwritesPreviousValue() {
        TenantContext.set("old");
        TenantContext.set("new");
        assertEquals("new", TenantContext.get());
    }
}

