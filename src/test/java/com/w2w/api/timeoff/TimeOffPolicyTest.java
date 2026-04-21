package com.w2w.api.timeoff;

import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class TimeOffPolicyTest {

    private LoginRepository loginRepository;
    private ManagerPermissionsRepository managerPermissionsRepository;
    private TimeOffPolicy timeOffPolicy;

    @BeforeEach
    void setUp() {
        loginRepository = Mockito.mock(LoginRepository.class);
        managerPermissionsRepository = Mockito.mock(ManagerPermissionsRepository.class);
        timeOffPolicy = new TimeOffPolicy(loginRepository, managerPermissionsRepository);
    }

    @Test
    void canCreateForEmployeeAllowsEmployeeForSelf() {
        User employeeUser = buildUser(100, "Employee", 11);
        when(loginRepository.findByLoginId("alice")).thenReturn(Optional.of(employeeUser));

        Authentication authentication = new UsernamePasswordAuthenticationToken("alice", null, List.of());

        assertTrue(timeOffPolicy.canCreateForEmployee(11, authentication));
        assertFalse(timeOffPolicy.canCreateForEmployee(12, authentication));
    }

    @Test
    void canCreateForEmployeeAllowsManagerForAnyEmployee() {
        User managerUser = buildUser(101, "Manager", 21);
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(managerUser));

        Authentication authentication = new UsernamePasswordAuthenticationToken("manager", null, List.of());

        assertTrue(timeOffPolicy.canCreateForEmployee(11, authentication));
        assertTrue(timeOffPolicy.canManage(authentication));
    }

    @Test
    void canCreateForEmployeeAllowsAddManagerWhenApproveTimeOffPermissionIsEnabled() {
        User addManagerUser = buildUser(102, "AddManager", 31);
        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanApproveTimeOff(true);

        when(loginRepository.findByLoginId("addmanager")).thenReturn(Optional.of(addManagerUser));
        when(managerPermissionsRepository.findByUserId(102)).thenReturn(Optional.of(permissions));

        Authentication authentication = new UsernamePasswordAuthenticationToken("addmanager", null, List.of());

        assertTrue(timeOffPolicy.canCreateForEmployee(11, authentication));
        assertTrue(timeOffPolicy.canManage(authentication));
    }

    @Test
    void canCreateForEmployeeRejectsAddManagerWithoutApproveTimeOffPermission() {
        User addManagerUser = buildUser(103, "AddManager", 41);
        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanApproveTimeOff(false);

        when(loginRepository.findByLoginId("limited")).thenReturn(Optional.of(addManagerUser));
        when(managerPermissionsRepository.findByUserId(103)).thenReturn(Optional.of(permissions));

        Authentication authentication = new UsernamePasswordAuthenticationToken("limited", null, List.of());

        assertFalse(timeOffPolicy.canCreateForEmployee(11, authentication));
        assertFalse(timeOffPolicy.canManage(authentication));
    }

    private User buildUser(Integer userId, String roleName, Integer employeeId) {
        User user = new User();
        user.setId(userId);

        UserRole role = new UserRole();
        role.setName(roleName);
        user.setRole(role);

        if (employeeId != null) {
            com.w2w.api.employee.model.Employee employee = new com.w2w.api.employee.model.Employee();
            employee.setEmployeeId(employeeId);
            user.setEmployee(employee);
        }

        return user;
    }
}
