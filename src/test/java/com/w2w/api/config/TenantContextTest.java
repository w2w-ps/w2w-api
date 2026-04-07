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
    void resolveTenant_returnsCurrentTenantWhenPresent() {
        TenantContext.setCurrentTenant(7);

        assertEquals(7, TenantContext.resolveTenant(99));
    }

    @Test
    void resolveTenant_returnsMinusOneWhenCurrentTenantMissing() {
        assertEquals(-1, TenantContext.resolveTenant(99));
    }
}
