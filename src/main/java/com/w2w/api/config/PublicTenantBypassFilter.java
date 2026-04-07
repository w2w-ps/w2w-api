package com.w2w.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PublicTenantBypassFilter extends OncePerRequestFilter {

    private static final int PUBLIC_BYPASS_TENANT_ID = 0;

    private final RequestMatcher publicApiRequestMatcher;

    public PublicTenantBypassFilter(RequestMatcher publicApiRequestMatcher) {
        this.publicApiRequestMatcher = publicApiRequestMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean isPublicRequest = publicApiRequestMatcher.matches(request);

        try {
            if (isPublicRequest) {
                TenantContext.setCurrentTenant(PUBLIC_BYPASS_TENANT_ID);
            }

            filterChain.doFilter(request, response);
        } finally {
            if (isPublicRequest) {
                TenantContext.clear();
            }
        }
    }
}
