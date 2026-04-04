package com.dealer.dealer_inventory.security;

/**
 * Thread-local holder for the current tenant ID extracted from the X-Tenant-Id header.
 * Cleared automatically at the end of each request by {@link TenantAuthenticationFilter}.
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() { }

    public static void set(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String get() {
        return TENANT.get();
    }

    public static String require() {
        String t = TENANT.get();
        if (t == null) {
            throw new com.dealer.dealer_inventory.exception.MissingTenantException();
        }
        return t;
    }

    public static void clear() {
        TENANT.remove();
    }
}

