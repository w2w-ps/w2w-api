package com.w2w.api.position;

import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("positionPolicy")
public class PositionPolicy {

    private final LoginRepository loginRepository;
    private final ManagerPermissionsRepository managerPermissionsRepository;

    public PositionPolicy(
            LoginRepository loginRepository,
            ManagerPermissionsRepository managerPermissionsRepository
    ) {
        this.loginRepository = loginRepository;
        this.managerPermissionsRepository = managerPermissionsRepository;
    }

    public boolean canManage(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return false;
        }

        User currentUser = loginRepository.findByLoginId(authentication.getName()).orElse(null);
        if (currentUser == null) {
            return false;
        }

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getName() : null;
        if ("Manager".equalsIgnoreCase(roleName)) {
            return true;
        }

        if ("AddManager".equalsIgnoreCase(roleName)) {
            return managerPermissionsRepository.findByUserId(currentUser.getId())
                    .map(ManagerPermissions::isCanManagePositions)
                    .orElse(false);
        }

        return false;
    }
}
