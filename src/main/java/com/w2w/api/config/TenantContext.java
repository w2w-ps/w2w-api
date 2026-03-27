package com.w2w.api.config;

public final class TenantContext {
    private static final ThreadLocal<Integer> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(Integer tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Integer getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
