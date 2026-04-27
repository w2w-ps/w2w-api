package com.w2w.api.categorygroup;

import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.categorygroup.dto.CategoryGroupSummary;
import com.w2w.api.categorygroup.dto.UpdateCategoryGroupRequest;
import com.w2w.api.categorygroup.model.CategoryGroup;
import com.w2w.api.categorygroup.repository.CategoryGroupRepository;
import com.w2w.api.config.TenantContext;
import com.w2w.api.config.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryGroupServiceTest {

    @Mock
    private CategoryGroupRepository categoryGroupRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryGroupService categoryGroupService;

    private Category floor;
    private Category front;
    private CategoryGroup categoryGroup;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1);
        floor = new Category();
        floor.setCategoryId(4);
        floor.setCompanyId(1);
        floor.setDescription("Floor");
        floor.setShortDesc("FLR");
        floor.setIsDeleted(false);

        front = new Category();
        front.setCategoryId(5);
        front.setCompanyId(1);
        front.setDescription("Front");
        front.setShortDesc("FRT");
        front.setIsDeleted(false);

        categoryGroup = new CategoryGroup();
        categoryGroup.setGroupId(301);
        categoryGroup.setCompanyId(1);
        categoryGroup.setDescription("Standard Shifts");
        categoryGroup.setIsDeleted(false);
        categoryGroup.setCategories(List.of(floor));
    }

    @Test
    void getCategoryGroups_returnsMappedGroups() {
        when(categoryGroupRepository.findByCompanyId(1)).thenReturn(List.of(categoryGroup));

        List<CategoryGroupSummary> result = categoryGroupService.getCategoryGroups("all");

        assertEquals(1, result.size());
        assertEquals(301, result.get(0).id());
        assertEquals("Standard Shifts", result.get(0).name());
        assertEquals(1, result.get(0).categories().size());
        assertEquals(4, result.get(0).categories().get(0).id());
        verify(categoryGroupRepository).findByCompanyId(1);
    }

    @Test
    void getCategoryGroups_returnsMappedActiveGroups() {
        when(categoryGroupRepository.findByCompanyIdAndIsDeletedFalse(1)).thenReturn(List.of(categoryGroup));

        List<CategoryGroupSummary> result = categoryGroupService.getCategoryGroups("active");

        assertEquals(1, result.size());
        assertEquals(301, result.get(0).id());
        verify(categoryGroupRepository).findByCompanyIdAndIsDeletedFalse(1);
    }

    @Test
    void getCategoryGroups_returnsMappedInactiveGroups() {
        categoryGroup.setIsDeleted(true);
        when(categoryGroupRepository.findByCompanyIdAndIsDeletedTrue(1)).thenReturn(List.of(categoryGroup));

        List<CategoryGroupSummary> result = categoryGroupService.getCategoryGroups("inactive");

        assertEquals(1, result.size());
        assertEquals(301, result.get(0).id());
        verify(categoryGroupRepository).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getCategoryGroups_throwsWhenStatusUnsupported() {
        assertThrows(ResponseStatusException.class, () -> categoryGroupService.getCategoryGroups("archived"));

        verify(categoryGroupRepository, never()).findByCompanyId(1);
        verify(categoryGroupRepository, never()).findByCompanyIdAndIsDeletedFalse(1);
        verify(categoryGroupRepository, never()).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getCategoryGroupById_returnsMappedGroup() {
        when(categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(301, 1)).thenReturn(Optional.of(categoryGroup));

        Optional<CategoryGroupSummary> result = categoryGroupService.getCategoryGroupById(301);

        assertTrue(result.isPresent());
        assertEquals("Standard Shifts", result.get().name());
        verify(categoryGroupRepository).findByGroupIdAndCompanyIdAndIsDeletedFalse(301, 1);
    }

    @Test
    void createCategoryGroup_savesResolvedCategories() {
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 2 && categoryIds.containsAll(List.of(5, 4))),
                eq(1)
        ))
                .thenReturn(List.of(floor, front));

        categoryGroupService.createCategoryGroup("Standard Shifts", List.of(5, 4));

        verify(categoryRepository).findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 2 && categoryIds.containsAll(List.of(5, 4))),
                eq(1)
        );
        verify(categoryGroupRepository).save(argThat(group ->
                group.getCompanyId().equals(1)
                        && group.getDescription().equals("Standard Shifts")
                        && Boolean.FALSE.equals(group.getIsDeleted())
                        && group.getCategories().stream().map(Category::getCategoryId).toList().equals(List.of(5, 4))));
    }

    @Test
    void createCategoryGroup_throwsWhenAnyCategoryIsMissing() {
        List<Integer> requestedCategoryIds = List.of(4, 999);
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 2 && categoryIds.containsAll(List.of(4, 999))),
                eq(1)
        ))
                .thenReturn(List.of(floor));

        assertThrows(ResponseStatusException.class,
                () -> categoryGroupService.createCategoryGroup("Standard Shifts", requestedCategoryIds));

        verify(categoryGroupRepository, never()).save(any(CategoryGroup.class));
    }

    @Test
    void updateCategoryGroup_replacesGroupMembership() {
        UpdateCategoryGroupRequest request = new UpdateCategoryGroupRequest("Updated Standard Shifts", List.of(5));
        when(categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(301, 1)).thenReturn(Optional.of(categoryGroup));
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 1 && categoryIds.contains(5)),
                eq(1)
        ))
                .thenReturn(List.of(front));

        categoryGroupService.updateCategoryGroup(301, request);

        assertEquals("Updated Standard Shifts", categoryGroup.getDescription());
        assertEquals(1, categoryGroup.getCategories().size());
        assertEquals(5, categoryGroup.getCategories().get(0).getCategoryId());
        verify(categoryGroupRepository).save(categoryGroup);
    }

    @Test
    void deleteCategoryGroup_softDeletesGroup() {
        when(categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(301, 1)).thenReturn(Optional.of(categoryGroup));

        categoryGroupService.deleteCategoryGroup(301);

        assertTrue(categoryGroup.getIsDeleted());
        verify(categoryGroupRepository).save(categoryGroup);
    }

    @Test
    void deleteCategoryGroup_throwsWhenMissing() {
        when(categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(301, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryGroupService.deleteCategoryGroup(301));

        verify(categoryGroupRepository, never()).save(any(CategoryGroup.class));
    }

    @Test
    void createCategoryGroup_throwsWhenTenantMissing() {
        List<Integer> categoryIds = List.of(4, 5);
        TenantContext.clear();

        assertThrows(ResponseStatusException.class,
                () -> categoryGroupService.createCategoryGroup("Standard Shifts", categoryIds));
        verify(categoryGroupRepository, never()).save(any(CategoryGroup.class));
    }
}
