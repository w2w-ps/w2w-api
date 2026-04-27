package com.w2w.api.category;

import com.w2w.api.category.dto.UpdateCategoryRequest;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryRepository;
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

@SpringJUnitConfig(CategoryServiceAuthorizationTest.MethodSecurityTestConfig.class)
class CategoryServiceAuthorizationTest {

    @Configuration
    @EnableMethodSecurity
    @Import({CategoryService.class, CategoryPolicy.class})
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private CategoryService categoryService;

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
    void manager_canManageCategoryMutations() {
        authenticate("manager");
        when(loginRepository.findByLoginId("manager")).thenReturn(Optional.of(userWithRole(1, "manager", "Manager")));
        stubCategoryLookups();

        assertDoesNotThrow(() -> categoryService.create("Host", "Description", "09:00", "17:00", 10, (short) 1));
        assertDoesNotThrow(() -> categoryService.update(101, new UpdateCategoryRequest("Lead", "Description", "09:00", "17:00", 10, (short) 2)));
        assertDoesNotThrow(() -> categoryService.delete(101));

        verify(categoryRepository, times(3)).save(any(Category.class));
    }

    @Test
    void additionalManagerWithPermission_canManageCategoryMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));

        ManagerPermissions permissions = new ManagerPermissions();
        permissions.setCanManageCategories(true);
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.of(permissions));
        stubCategoryLookups();

        assertDoesNotThrow(() -> categoryService.create("Host", "Description", "09:00", "17:00", 10, (short) 1));
        assertDoesNotThrow(() -> categoryService.update(101, new UpdateCategoryRequest("Lead", "Description", "09:00", "17:00", 10, (short) 2)));
        assertDoesNotThrow(() -> categoryService.delete(101));

        verify(categoryRepository, times(3)).save(any(Category.class));
    }

    @Test
    void additionalManagerWithoutPermission_cannotManageCategoryMutations() {
        authenticate("additional-manager");
        when(loginRepository.findByLoginId("additional-manager"))
                .thenReturn(Optional.of(userWithRole(2, "additional-manager", "AddManager")));
        when(managerPermissionsRepository.findByUserId(2)).thenReturn(Optional.empty());

        assertDeniedForAllMutations();
    }

    @Test
    void employee_cannotManageCategoryMutations() {
        authenticate("employee");
        when(loginRepository.findByLoginId("employee")).thenReturn(Optional.of(userWithRole(3, "employee", "Employee")));

        assertDeniedForAllMutations();
    }

    private void assertDeniedForAllMutations() {
        UpdateCategoryRequest updateRequest = new UpdateCategoryRequest("Lead", "Description", "09:00", "17:00", 10, (short) 2);

        assertThrows(AccessDeniedException.class, () -> categoryService.create("Host", "Description", "09:00", "17:00", 10, (short) 1));
        assertThrows(AccessDeniedException.class, () -> categoryService.update(101, updateRequest));
        assertThrows(AccessDeniedException.class, () -> categoryService.delete(101));

        verify(categoryRepository, never()).save(any(Category.class));
        verify(categoryRepository, never()).findByCategoryIdAndCompanyIdAndIsDeletedFalse(any(), any());
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    private void stubCategoryLookups() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(101, 1))
                .thenReturn(Optional.of(activeCategory()), Optional.of(activeCategory()));
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
