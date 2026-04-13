package com.w2w.api.category;

import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("categoryPolicy")
public class CategoryPolicy {

    private final LoginRepository loginRepository;
    private final ManagerPermissionsRepository managerPermissionsRepository;

    public CategoryPolicy(
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
                    .map(ManagerPermissions::isCanManageCategories)
                    .orElse(false);
        }

        return false;
    }
}
