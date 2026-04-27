package com.w2w.api.categorygroup;

import com.w2w.api.category.CategoryPolicy;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.categorygroup.dto.UpdateCategoryGroupRequest;
import com.w2w.api.categorygroup.model.CategoryGroup;
import com.w2w.api.categorygroup.repository.CategoryGroupRepository;
import com.w2w.api.config.TenantContext;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRole;
import com.w2w.api.manager.ManagerPermissions;
import com.w2w.api.manager.ManagerPermissionsRepository;
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

@SpringJUnitConfig(CategoryGroupServiceAuthorizationTest.MethodSecurityTestConfig.class)
class CategoryGroupServiceAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    @Import({CategoryGroupService.class, CategoryPolicy.class})
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private CategoryGroupService categoryGroupService;

    @MockitoBean
    private CategoryGroupRepository categoryGroupRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

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
    void manager_canManageCategoryGroupMutations() {
        authenticate("manager");
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(userWithRole(1, "manager", "Manager")));
        stubCategoryGroupLookups();

        assertDoesNotThrow(() -> categoryGroupService.createCategoryGroup("Standard Shifts", List.of(101)));
        assertDoesNotThrow(() -> categoryGroupService.updateCategoryGroup(201, new UpdateCategoryGroupRequest("Updated Group", List.of(101))));
        assertDoesNotThrow(() -> categoryGroupService.deleteCategoryGroup(201));

        verify(categoryGroupRepository, times(3)).save(any(CategoryGroup.class));
    }

    @Test
    void additionalManagerWithPermission_canManageCategoryGroupMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));

        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanManageCategories(true);
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.of(permissions));
        stubCategoryGroupLookups();

        assertDoesNotThrow(() -> categoryGroupService.createCategoryGroup("Standard Shifts", List.of(101)));
        assertDoesNotThrow(() -> categoryGroupService.updateCategoryGroup(201, new UpdateCategoryGroupRequest("Updated Group", List.of(101))));
        assertDoesNotThrow(() -> categoryGroupService.deleteCategoryGroup(201));

        verify(categoryGroupRepository, times(3)).save(any(CategoryGroup.class));
    }

    @Test
    void additionalManagerWithoutPermission_cannotManageCategoryGroupMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.empty());

        assertDeniedForAllMutations();
    }

    @Test
    void employee_cannotManageCategoryGroupMutations() {
        authenticate("employee");
        when(loginRepository.findByLoginId("employee")).thenReturn(Optional.of(userWithRole(3, "employee", "Employee")));

        assertDeniedForAllMutations();
    }

    private void assertDeniedForAllMutations() {
        List<Integer> categoryIds = List.of(101);
        UpdateCategoryGroupRequest updateRequest = new UpdateCategoryGroupRequest("Updated Group", categoryIds);

        assertThrows(AccessDeniedException.class, () -> categoryGroupService.createCategoryGroup("Standard Shifts", categoryIds));
        assertThrows(AccessDeniedException.class, () -> categoryGroupService.updateCategoryGroup(201, updateRequest));
        assertThrows(AccessDeniedException.class, () -> categoryGroupService.deleteCategoryGroup(201));

        verify(categoryGroupRepository, never()).save(any(CategoryGroup.class));
        verify(categoryGroupRepository, never()).findByGroupIdAndCompanyIdAndIsDeletedFalse(any(), any());
        verify(categoryRepository, never()).findByCategoryIdInAndCompanyIdAndIsDeletedFalse(any(), any());
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    private void stubCategoryGroupLookups() {
        Category category = activeCategory();
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(any(), any()))
                .thenReturn(List.of(category), List.of(category));
        when(categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1))
                .thenReturn(Optional.of(activeCategoryGroup()), Optional.of(activeCategoryGroup()));
    }

    private Category activeCategory() {
        Category category = new Category();
        category.setCategoryId(101);
        category.setCompanyId(1);
        category.setDescription("Floor");
        category.setShortDesc("FLR");
        category.setIsDeleted(false);
        return category;
    }

    private CategoryGroup activeCategoryGroup() {
        CategoryGroup group = new CategoryGroup();
        group.setGroupId(201);
        group.setCompanyId(1);
        group.setDescription("Standard Shifts");
        group.setIsDeleted(false);
        group.setCategories(List.of(activeCategory()));
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
