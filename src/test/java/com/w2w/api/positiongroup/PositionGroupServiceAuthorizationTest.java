package com.w2w.api.positiongroup;

import com.w2w.api.config.TenantContext;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import com.w2w.api.position.PositionPolicy;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.positiongroup.dto.UpdatePositionGroupRequest;
import com.w2w.api.positiongroup.model.PositionGroup;
import com.w2w.api.positiongroup.repository.PositionGroupRepository;
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

@SpringJUnitConfig(PositionGroupServiceAuthorizationTest.MethodSecurityTestConfig.class)
class PositionGroupServiceAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    @Import({PositionGroupService.class, PositionPolicy.class})
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private PositionGroupService positionGroupService;

    @MockitoBean
    private PositionGroupRepository positionGroupRepository;

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
    void manager_canManagePositionGroupMutations() {
        authenticate("manager");
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(userWithRole(1, "manager", "Manager")));
        stubPositionGroupLookups();

        assertDoesNotThrow(() -> positionGroupService.createPositionGroup("Front of House", List.of(101)));
        assertDoesNotThrow(() -> positionGroupService.updatePositionGroup(201, new UpdatePositionGroupRequest("Updated Group", List.of(101))));
        assertDoesNotThrow(() -> positionGroupService.deletePositionGroup(201));

        verify(positionGroupRepository, times(3)).save(any(PositionGroup.class));
    }

    @Test
    void additionalManagerWithPermission_canManagePositionGroupMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));

        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanManagePositions(true);
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.of(permissions));
        stubPositionGroupLookups();

        assertDoesNotThrow(() -> positionGroupService.createPositionGroup("Front of House", List.of(101)));
        assertDoesNotThrow(() -> positionGroupService.updatePositionGroup(201, new UpdatePositionGroupRequest("Updated Group", List.of(101))));
        assertDoesNotThrow(() -> positionGroupService.deletePositionGroup(201));

        verify(positionGroupRepository, times(3)).save(any(PositionGroup.class));
    }

    @Test
    void additionalManagerWithoutPermission_cannotManagePositionGroupMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.empty());

        assertDeniedForAllMutations();
    }

    @Test
    void employee_cannotManagePositionGroupMutations() {
        authenticate("employee");
        when(loginRepository.findByLoginId("employee")).thenReturn(Optional.of(userWithRole(3, "employee", "Employee")));

        assertDeniedForAllMutations();
    }

    private void assertDeniedForAllMutations() {
        assertThrows(AccessDeniedException.class, () -> positionGroupService.createPositionGroup("Front of House", List.of(101)));
        assertThrows(AccessDeniedException.class, () -> positionGroupService.updatePositionGroup(201, new UpdatePositionGroupRequest("Updated Group", List.of(101))));
        assertThrows(AccessDeniedException.class, () -> positionGroupService.deletePositionGroup(201));

        verify(positionGroupRepository, never()).save(any(PositionGroup.class));
        verify(positionGroupRepository, never()).findByGroupIdAndCompanyIdAndIsDeletedFalse(any(), any());
        verify(positionRepository, never()).findByPositionIdInAndCompanyId(any(), any());
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    private void stubPositionGroupLookups() {
        Position position = activePosition();
        when(positionRepository.findByPositionIdInAndCompanyId(any(), any()))
                .thenReturn(List.of(position), List.of(position));
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1))
                .thenReturn(Optional.of(activePositionGroup()), Optional.of(activePositionGroup()));
    }

    private Position activePosition() {
        Position position = new Position();
        position.setPositionId(101);
        position.setCompanyId(1);
        position.setDescription("Server");
        position.setIsDeleted(false);
        return position;
    }

    private PositionGroup activePositionGroup() {
        PositionGroup group = new PositionGroup();
        group.setGroupId(201);
        group.setCompanyId(1);
        group.setDescription("Front of House");
        group.setPositions(List.of(activePosition()));
        group.setIsDeleted(false);
        return group;
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
