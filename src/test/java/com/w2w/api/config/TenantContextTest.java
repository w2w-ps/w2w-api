package com.w2w.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantContextTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getCurrentTenant_returnsCurrentTenantWhenPresent() {
        TenantContext.setCurrentTenant(7);

        assertEquals(7, TenantContext.getCurrentTenant());
    }

    @Test
    void getCurrentTenant_returnsMinusOneWhenCurrentTenantMissing() {
        assertEquals(-1, TenantContext.getCurrentTenant());
    }
}
