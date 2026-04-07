package com.w2w.api.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PublicTenantBypassFilterTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void matchedRequest_setsPublicBypassTenantDuringFilterChainAndClearsAfter() throws ServletException, IOException {
        PublicTenantBypassFilter filter = new PublicTenantBypassFilter(PathPatternRequestMatcher.pathPattern("/api/login"));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> assertEquals(0, TenantContext.getCurrentTenant()));

        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void unmatchedRequest_doesNotSetBypassTenant() throws ServletException, IOException {
        PublicTenantBypassFilter filter = new PublicTenantBypassFilter(PathPatternRequestMatcher.pathPattern("/api/login"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/positions");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> assertNull(TenantContext.getCurrentTenant()));

        assertNull(TenantContext.getCurrentTenant());
    }
}
