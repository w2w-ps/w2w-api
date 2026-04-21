package com.w2w.api.timeoff;

import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("timeOffPolicy")
public class TimeOffPolicy {

    private final LoginRepository loginRepository;
    private final ManagerPermissionsRepository managerPermissionsRepository;

    public TimeOffPolicy(
            LoginRepository loginRepository,
            ManagerPermissionsRepository managerPermissionsRepository
    ) {
        this.loginRepository = loginRepository;
        this.managerPermissionsRepository = managerPermissionsRepository;
    }

    public boolean canCreateForEmployee(Integer employeeId, Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        if (currentUser == null || employeeId == null) {
            return false;
        }

        if (canManage(currentUser)) {
            return true;
        }

        Integer currentEmployeeId = currentUser.getEmployeeId();
        return currentEmployeeId != null && currentEmployeeId.equals(employeeId);
    }

    public boolean canManage(Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication);
        return canManage(currentUser);
    }

    private User resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        return loginRepository.findByLoginId(authentication.getName()).orElse(null);
    }

    private boolean canManage(User currentUser) {
        if (currentUser == null) {
            return false;
        }

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if ("Manager".equalsIgnoreCase(roleName)) {
            return true;
        }

        if ("AddManager".equalsIgnoreCase(roleName)) {
            return managerPermissionsRepository.findByUserId(currentUser.getId())
                    .map(ManagerPermissions::isCanApproveTimeOff)
                    .orElse(false);
        }

        return false;
    }
}