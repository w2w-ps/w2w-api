package com.w2w.api.config;

import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.login.LoginController;
import com.w2w.api.login.LoginService;
import com.w2w.api.login.PasswordValidationResponse;
import com.w2w.api.position.PositionController;
import com.w2w.api.position.PositionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({LoginController.class, PositionController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private PositionService positionService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void allowJwtFilterToContinueChain() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void loginEndpoint_isPublic() throws Exception {
        when(loginService.authenticate("user", "bad-password")).thenAnswer(invocation -> {
            assertEquals(0, TenantContext.getCurrentTenant());
            return Optional.empty();
        });

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user",
                                  "password": "bad-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonPermittedEndpoint_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/positions").param("companyId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUser_canAccessProtectedEndpoint() throws Exception {
        when(positionService.getPositions(1, "all")).thenReturn(List.of());

        mockMvc.perform(get("/api/positions")
                        .param("companyId", "1")
                        .with(user("employee").authorities(() -> "Employee")))
                .andExpect(status().isOk());
    }

    @Test
    void resetUserAccount_requiresManagerAuthority() throws Exception {
        mockMvc.perform(post("/api/login/reset-user-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentUsername": "current",
                                  "newUsername": "new-user",
                                  "newPassword": "Secret123!",
                                  "confirmPassword": "Secret123!"
                                }
                                """)
                        .with(user("employee").authorities(() -> "Employee")))
                .andExpect(status().isForbidden());
    }

    @Test
    void manager_canAccessResetUserAccount() throws Exception {
        when(loginService.resetUserAccount(any(), any(), any(), any()))
                .thenReturn(new PasswordValidationResponse(true, List.of(), "User account reset successfully."));

        mockMvc.perform(post("/api/login/reset-user-account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentUsername": "current",
                                  "newUsername": "new-user",
                                  "newPassword": "Secret123!",
                                  "confirmPassword": "Secret123!"
                                }
                                """)
                        .with(user("manager").authorities(() -> "Manager")))
                .andExpect(status().isOk());
    }

    @Test
    void updatePassword_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/login/update-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "user",
                                  "oldPassword": "OldSecret123!",
                                  "newPassword": "NewSecret123!",
                                  "confirmPassword": "NewSecret123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
