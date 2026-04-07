package com.w2w.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TenantDatabaseConfigTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void resolveDatabaseTenantId_returnsMinusOneWithoutTenantContext() {
        int tenantId = TenantDatabaseConfig.resolveDatabaseTenantId();

        assertEquals(-1, tenantId);
        assertFalse(TenantDatabaseConfig.isInternalSystemLookup(tenantId));
    }

    @Test
    void resolveDatabaseTenantId_returnsZeroForExplicitPublicBypass() {
        TenantContext.setCurrentTenant(0);

        int tenantId = TenantDatabaseConfig.resolveDatabaseTenantId();

        assertEquals(0, tenantId);
        assertTrue(TenantDatabaseConfig.isInternalSystemLookup(tenantId));
    }

    @Test
    void resolveDatabaseTenantId_keepsAuthenticatedTenantWithoutBypass() {
        TenantContext.setCurrentTenant(4);

        int tenantId = TenantDatabaseConfig.resolveDatabaseTenantId();

        assertEquals(4, tenantId);
        assertFalse(TenantDatabaseConfig.isInternalSystemLookup(tenantId));
    }
}
