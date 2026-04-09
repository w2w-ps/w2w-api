package com.w2w.api.position;

import com.w2w.api.config.TenantContext;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(PositionServiceAuthorizationTest.MethodSecurityTestConfig.class)
class PositionServiceAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    @Import({PositionService.class, PositionPolicy.class})
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private PositionService positionService;

    @MockitoBean
    private PositionRepository positionRepository;

    @MockitoBean
    private LoginRepository loginRepository;

    @MockitoBean
    private ManagerPermissionsRepository managerPermissionsRepository;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1);
    }

    @AfterEach
    void clearContexts() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void manager_canManagePositionMutations() {
        authenticate("manager");
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(userWithRole(1, "manager", "Manager")));
        stubPositionLookups();

        assertDoesNotThrow(() -> positionService.create("Host"));
        assertDoesNotThrow(() -> positionService.update(101, new UpdatePositionRequest("Lead Server")));
        assertDoesNotThrow(() -> positionService.delete(101));
        assertDoesNotThrow(() -> positionService.restore(101));

        verify(positionRepository, times(4)).save(any(Position.class));
    }

    @Test
    void additionalManagerWithPermission_canManagePositionMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));

        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanManagePositions(true);
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.of(permissions));
        stubPositionLookups();

        assertDoesNotThrow(() -> positionService.create("Host"));
        assertDoesNotThrow(() -> positionService.update(101, new UpdatePositionRequest("Lead Server")));
        assertDoesNotThrow(() -> positionService.delete(101));
        assertDoesNotThrow(() -> positionService.restore(101));

        verify(positionRepository, times(4)).save(any(Position.class));
    }

    @Test
    void additionalManagerWithoutPermission_cannotManagePositionMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.empty());

        assertDeniedForAllMutations();
    }

    @Test
    void employee_cannotManagePositionMutations() {
        authenticate("employee");
        when(loginRepository.findByLoginId("employee")).thenReturn(Optional.of(userWithRole(3, "employee", "Employee")));

        assertDeniedForAllMutations();
    }

    private void assertDeniedForAllMutations() {
        assertThrows(AccessDeniedException.class, () -> positionService.create("Host"));
        assertThrows(AccessDeniedException.class, () -> positionService.update(101, new UpdatePositionRequest("Lead Server")));
        assertThrows(AccessDeniedException.class, () -> positionService.delete(101));
        assertThrows(AccessDeniedException.class, () -> positionService.restore(101));

        verify(positionRepository, never()).save(any(Position.class));
        verify(positionRepository, never()).findByPositionIdAndCompanyId(any(), any());
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    private void stubPositionLookups() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(
                Optional.of(activePosition()),
                Optional.of(activePosition()),
                Optional.of(deletedPosition())
        );
    }

    private Position activePosition() {
        Position position = new Position();
        position.setPositionId(101);
        position.setCompanyId(1);
        position.setDescription("Server");
        position.setIsDeleted(false);
        return position;
    }

    private Position deletedPosition() {
        Position position = activePosition();
        position.setIsDeleted(true);
        return position;
    }

    private User userWithRole(Integer id, String username, String roleName) {
        User user = new User();
        UserRole role = new UserRole();
        role.setName(roleName);
        user.setId(id);
        user.setLoginId(username);
        user.setRole(role);
        return user;
    }
}
