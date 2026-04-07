package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryResponse;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryGroupRepository;
import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.config.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryGroupRepository categoryGroupRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1);

        category = new Category();
        category.setCategoryId(1);
        category.setCompanyId(1);
        category.setDescription("Description");
        category.setShortDesc("ShortName");
        category.setStartTime("09:00");
        category.setEndTime("17:00");
        category.setPositionId(12);
        category.setColor((short) 1);
        category.setIsDeleted(false);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getCategories_all_returnsAllCategories() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(2);
        deletedCategory.setDescription("Deleted Category");
        deletedCategory.setIsDeleted(true);

        when(categoryRepository.findByCompanyId(1)).thenReturn(List.of(category, deletedCategory));

        List<CategorySummary> result = categoryService.getCategories("all");

        assertEquals(2, result.size());
        verify(categoryRepository).findByCompanyId(1);
    }

    @Test
    void getCategories_active_returnsActiveCategories() {
        when(categoryRepository.findByCompanyIdAndIsDeletedFalse(1)).thenReturn(List.of(category));

        List<CategorySummary> result = categoryService.getCategories("active");

        assertEquals(1, result.size());
        assertEquals("Description", result.getFirst().description());
        verify(categoryRepository).findByCompanyIdAndIsDeletedFalse(1);
    }

    @Test
    void getCategories_inactive_returnsInactiveCategories() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(2);
        deletedCategory.setDescription("Deleted Category");
        deletedCategory.setIsDeleted(true);

        when(categoryRepository.findByCompanyIdAndIsDeletedTrue(1)).thenReturn(List.of(deletedCategory));

        List<CategorySummary> result = categoryService.getCategories("inactive");

        assertEquals(1, result.size());
        assertEquals("Deleted Category", result.getFirst().description());
        verify(categoryRepository).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getCategoriesByCompanyId_returnsAllCategories() {
        when(categoryRepository.findByCompanyId(1)).thenReturn(List.of(category));

        List<CategorySummary> result = categoryService.getCategoriesByCompanyId();

        assertEquals(1, result.size());
        verify(categoryRepository).findByCompanyId(1);
    }

    @Test
    void getCategories_unsupportedStatus_throwsBadRequest() {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> categoryService.getCategories("archived"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getCategoryById_returnsCategoryResponse() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, 1)).thenReturn(Optional.of(category));

        Optional<CategoryResponse> result = categoryService.getCategoryById(1);

        assertTrue(result.isPresent());
        assertEquals(12, result.get().positionId());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, 1);
    }

    @Test
    void createCategory_savesNewCategory() {
        categoryService.createCategory("ShortName", "Description", "09:00", "17:00", 12, (short) 1);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_withoutTenant_throwsUnauthorized() {
        TenantContext.clear();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.createCategory("ShortName", "Description", "09:00", "17:00", 12, (short) 1)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_updatesAndSavesCategory() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, 1)).thenReturn(Optional.of(category));

        categoryService.updateCategory(1, new com.w2w.api.category.dto.UpdateCategoryRequest(
                "NewShortName",
                "New Description",
                "10:00",
                "18:00",
                13,
                (short) 2
        ));

        assertEquals("NewShortName", category.getShortDesc());
        assertEquals(13, category.getPositionId());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_notFound_throwsNotFound() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, 1)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> categoryService.updateCategory(1, new com.w2w.api.category.dto.UpdateCategoryRequest(
                        "NewShortName",
                        "New Description",
                        "10:00",
                        "18:00",
                        13,
                        (short) 2
                ))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_setsIsDeletedToTrue() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, 1)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1);

        assertTrue(category.getIsDeleted());
        verify(categoryRepository).save(category);
    }
}
