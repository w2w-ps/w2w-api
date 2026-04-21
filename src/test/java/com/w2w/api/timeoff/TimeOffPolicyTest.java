package com.w2w.api.timeoff;

import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.config.TenantContext;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
        timeOffPolicy = new TimeOffPolicy(loginRepository, managerPermissionsRepository, Mockito.mock(TimeOffRequestRepository.class));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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

    @Test
    void canCancelTimeOffRequestAllowsEmployeeForOwnRequest() {
        TimeOffRequestRepository timeOffRequestRepository = Mockito.mock(TimeOffRequestRepository.class);
        timeOffPolicy = new TimeOffPolicy(loginRepository, managerPermissionsRepository, timeOffRequestRepository);

        User employeeUser = buildUser(104, "Employee", 11);
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(5001);
        request.setCompanyId(1);
        request.setEmployeeId(11);

        when(loginRepository.findByLoginId("alice")).thenReturn(Optional.of(employeeUser));
        when(timeOffRequestRepository.findByRequestIdAndCompanyId(5001, 1)).thenReturn(Optional.of(request));

        TenantContext.setCurrentTenant(1);
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken("alice", null, List.of());
            assertTrue(timeOffPolicy.canCancelTimeOffRequest(5001, authentication));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void canCancelTimeOffRequestAllowsManagerForAnyRequest() {
        TimeOffRequestRepository timeOffRequestRepository = Mockito.mock(TimeOffRequestRepository.class);
        timeOffPolicy = new TimeOffPolicy(loginRepository, managerPermissionsRepository, timeOffRequestRepository);

        User managerUser = buildUser(105, "Manager", 21);
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(managerUser));

        TenantContext.setCurrentTenant(1);
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken("manager", null, List.of());
            assertTrue(timeOffPolicy.canCancelTimeOffRequest(5002, authentication));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void canCancelTimeOffRequestRejectsOtherEmployeeRequest() {
        TimeOffRequestRepository timeOffRequestRepository = Mockito.mock(TimeOffRequestRepository.class);
        timeOffPolicy = new TimeOffPolicy(loginRepository, managerPermissionsRepository, timeOffRequestRepository);

        User employeeUser = buildUser(106, "Employee", 11);
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(5003);
        request.setCompanyId(1);
        request.setEmployeeId(12);

        when(loginRepository.findByLoginId("alice")).thenReturn(Optional.of(employeeUser));
        when(timeOffRequestRepository.findByRequestIdAndCompanyId(5003, 1)).thenReturn(Optional.of(request));

        TenantContext.setCurrentTenant(1);
        try {
            Authentication authentication = new UsernamePasswordAuthenticationToken("alice", null, List.of());
            assertFalse(timeOffPolicy.canCancelTimeOffRequest(5003, authentication));
        } finally {
            TenantContext.clear();
        }
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
