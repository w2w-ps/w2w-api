package com.w2w.api.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentTenant {

    private CurrentTenant() {
    }

    public static Integer requireCurrentTenant() {
        Integer currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant == -1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant context required");
        }
        return currentTenant;
    }
}
