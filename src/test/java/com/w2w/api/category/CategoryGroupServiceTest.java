package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryGroupSummary;
import com.w2w.api.category.dto.CreateCategoryGroupRequest;
import com.w2w.api.category.dto.UpdateCategoryGroupRequest;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.model.CategoryGroup;
import com.w2w.api.category.repository.CategoryGroupRepository;
import com.w2w.api.category.repository.CategoryRepository;
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

    @BeforeEach
    void setUp() {
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
        categoryGroup.setCategories(List.of(floor));
    }

    @Test
    void getCategoryGroups_returnsMappedGroups() {
        when(categoryGroupRepository.findByCompanyId(1)).thenReturn(List.of(categoryGroup));

        List<CategoryGroupSummary> result = categoryGroupService.getCategoryGroups(1);

        assertEquals(1, result.size());
        assertEquals(301, result.get(0).id());
        assertEquals("Standard Shifts", result.get(0).name());
        assertEquals(1, result.get(0).categories().size());
        assertEquals(4, result.get(0).categories().get(0).id());
        verify(categoryGroupRepository).findByCompanyId(1);
    }

    @Test
    void getCategoryGroupById_returnsMappedGroup() {
        when(categoryGroupRepository.findByGroupIdAndCompanyId(301, 1)).thenReturn(Optional.of(categoryGroup));

        Optional<CategoryGroupSummary> result = categoryGroupService.getCategoryGroupById(301, 1);

        assertTrue(result.isPresent());
        assertEquals("Standard Shifts", result.get().name());
        verify(categoryGroupRepository).findByGroupIdAndCompanyId(301, 1);
    }

    @Test
    void createCategoryGroup_savesResolvedCategories() {
        CreateCategoryGroupRequest request = new CreateCategoryGroupRequest(1, "Standard Shifts", List.of(5, 4));
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 2 && categoryIds.containsAll(List.of(5, 4))),
                eq(1)
        ))
                .thenReturn(List.of(floor, front));

        categoryGroupService.createCategoryGroup(request);

        verify(categoryRepository).findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 2 && categoryIds.containsAll(List.of(5, 4))),
                eq(1)
        );
        verify(categoryGroupRepository).save(argThat(group ->
                group.getCompanyId().equals(1)
                        && group.getDescription().equals("Standard Shifts")
                        && group.getCategories().stream().map(Category::getCategoryId).toList().equals(List.of(5, 4))));
    }

    @Test
    void createCategoryGroup_throwsWhenAnyCategoryIsMissing() {
        CreateCategoryGroupRequest request = new CreateCategoryGroupRequest(1, "Standard Shifts", List.of(4, 999));
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 2 && categoryIds.containsAll(List.of(4, 999))),
                eq(1)
        ))
                .thenReturn(List.of(floor));

        assertThrows(ResponseStatusException.class, () -> categoryGroupService.createCategoryGroup(request));

        verify(categoryGroupRepository, never()).save(any(CategoryGroup.class));
    }

    @Test
    void updateCategoryGroup_replacesGroupMembership() {
        UpdateCategoryGroupRequest request = new UpdateCategoryGroupRequest("Updated Standard Shifts", List.of(5));
        when(categoryGroupRepository.findByGroupIdAndCompanyId(301, 1)).thenReturn(Optional.of(categoryGroup));
        when(categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(
                argThat(categoryIds -> categoryIds.size() == 1 && categoryIds.contains(5)),
                eq(1)
        ))
                .thenReturn(List.of(front));

        categoryGroupService.updateCategoryGroup(301, 1, request);

        assertEquals("Updated Standard Shifts", categoryGroup.getDescription());
        assertEquals(1, categoryGroup.getCategories().size());
        assertEquals(5, categoryGroup.getCategories().get(0).getCategoryId());
        verify(categoryGroupRepository).save(categoryGroup);
    }

    @Test
    void deleteCategoryGroup_removesMembershipsAndDeletesGroup() {
        when(categoryGroupRepository.findByGroupIdAndCompanyId(301, 1)).thenReturn(Optional.of(categoryGroup));

        categoryGroupService.deleteCategoryGroup(301, 1);

        verify(categoryGroupRepository).deleteCategoryMemberships(301);
        verify(categoryGroupRepository).delete(categoryGroup);
    }

    @Test
    void deleteCategoryGroup_throwsWhenMissing() {
        when(categoryGroupRepository.findByGroupIdAndCompanyId(301, 1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoryGroupService.deleteCategoryGroup(301, 1));

        verify(categoryGroupRepository, never()).deleteCategoryMemberships(any());
        verify(categoryGroupRepository, never()).delete(any(CategoryGroup.class));
    }
}
