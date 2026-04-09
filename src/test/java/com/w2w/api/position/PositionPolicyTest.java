package com.w2w.api.position;

import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionPolicyTest {

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private ManagerPermissionsRepository managerPermissionsRepository;

    @InjectMocks
    private PositionPolicy positionPolicy;

    @Test
    void manager_canManagePositions() {
        Authentication authentication = authenticate("manager");
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(userWithRole(1, "manager", "Manager")));

        assertTrue(positionPolicy.canManage(authentication));
    }

    @Test
    void additionalManagerWithPermission_canManagePositions() {
        Authentication authentication = authenticate("additional");
        when(loginRepository.findByLoginId("additional")).thenReturn(Optional.of(userWithRole(2, "additional", "AddManager")));

        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanManagePositions(true);
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.of(permissions));

        assertTrue(positionPolicy.canManage(authentication));
    }

    @Test
    void additionalManagerWithoutPermission_isForbidden() {
        Authentication authentication = authenticate("additional");
        when(loginRepository.findByLoginId("additional")).thenReturn(Optional.of(userWithRole(2, "additional", "AddManager")));
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.empty());

        assertFalse(positionPolicy.canManage(authentication));
    }

    @Test
    void employee_isForbidden() {
        Authentication authentication = authenticate("employee");
        when(loginRepository.findByLoginId("employee")).thenReturn(Optional.of(userWithRole(3, "employee", "Employee")));

        assertFalse(positionPolicy.canManage(authentication));
    }

    @Test
    void missingUser_isForbidden() {
        Authentication authentication = authenticate("missing");
        when(loginRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        assertFalse(positionPolicy.canManage(authentication));
    }

    private Authentication authenticate(String username) {
        return new UsernamePasswordAuthenticationToken(username, null, List.of());
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
