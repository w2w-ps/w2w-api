package com.w2w.api.login;

import com.w2w.api.config.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContexts() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenWithoutUser_returnsUnauthorizedBeforeChain() throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, loginRepository);
        MockHttpServletRequest request = requestWithBearerToken();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("missing-user");
        when(loginRepository.findAuthContextByLoginId("missing-user")).thenReturn(Optional.empty());

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("Your account is not associated with a company.", response.getErrorMessage());
        verify(filterChain, never()).doFilter(any(), any());
        verify(loginRepository, never()).findByLoginId(any());
    }

    @Test
    void validTokenWithUser_setsTenantAndAuthenticationBeforeChain() throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, loginRepository);
        MockHttpServletRequest request = requestWithBearerToken();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("manager");
        when(loginRepository.findAuthContextByLoginId("manager"))
                .thenReturn(Optional.of(new TestAuthContextProjection(7, "Manager")));
        doAnswer(invocation -> {
            assertEquals(7, TenantContext.getCurrentTenant());
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals("manager", SecurityContextHolder.getContext().getAuthentication().getName());
            return null;
        }).when(filterChain).doFilter(any(), any());

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals(-1, TenantContext.getCurrentTenant());
        verify(filterChain).doFilter(any(), any());
        verify(loginRepository, never()).findByLoginId(any());
    }

    @Test
    void validTokenWithUserAndNoRole_usesEmployeeAuthority() throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtUtil, loginRepository);
        MockHttpServletRequest request = requestWithBearerToken();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(jwtUtil.extractUsername("token")).thenReturn("employee");
        when(loginRepository.findAuthContextByLoginId("employee"))
                .thenReturn(Optional.of(new TestAuthContextProjection(9, null)));
        doAnswer(invocation -> {
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(
                    "ROLE_Employee",
                    SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority()
            );
            return null;
        }).when(filterChain).doFilter(any(), any());

        jwtAuthFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(any(), any());
        verify(loginRepository, never()).findByLoginId(any());
    }

    private MockHttpServletRequest requestWithBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private record TestAuthContextProjection(Integer companyId, String roleName) implements AuthContextProjection {
        @Override
        public Integer getCompanyId() {
            return companyId;
        }

        @Override
        public String getRoleName() {
            return roleName;
        }
    }
}
