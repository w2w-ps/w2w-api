package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryResponse;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.dto.CreateCategoryRequest;
import com.w2w.api.category.dto.UpdateCategoryRequest;
import com.w2w.api.category.model.Category;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(4);
        category.setCompanyId(1);
        category.setShortDesc("FLR");
        category.setDescription("Floor");
        category.setStartTime("08:00");
        category.setEndTime("17:00");
        category.setSkillId(12);
        category.setColor((short) 3);
        category.setIsDeleted(false);
    }

    @Test
    void getCategories_shouldReturnAllCategories() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(5);
        deletedCategory.setCompanyId(1);
        deletedCategory.setShortDesc("FRT");
        deletedCategory.setDescription("Front");
        deletedCategory.setIsDeleted(true);

        when(categoryRepository.findByCompanyId(1)).thenReturn(List.of(category, deletedCategory));

        List<CategorySummary> result = categoryService.getCategories(1, "all");

        assertEquals(2, result.size());
        assertEquals("Floor", result.get(0).name());
        assertEquals("Front", result.get(1).name());
        verify(categoryRepository).findByCompanyId(1);
    }

    @Test
    void getCategories_shouldReturnActiveCategories() {
        when(categoryRepository.findByCompanyIdAndIsDeletedFalse(1)).thenReturn(List.of(category));

        List<CategorySummary> result = categoryService.getCategories(1, "active");

        assertEquals(1, result.size());
        assertEquals(4, result.get(0).id());
        verify(categoryRepository).findByCompanyIdAndIsDeletedFalse(1);
    }

    @Test
    void getCategories_shouldReturnInactiveCategories() {
        category.setIsDeleted(true);
        when(categoryRepository.findByCompanyIdAndIsDeletedTrue(1)).thenReturn(List.of(category));

        List<CategorySummary> result = categoryService.getCategories(1, "inactive");

        assertEquals(1, result.size());
        assertEquals(4, result.get(0).id());
        verify(categoryRepository).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getCategories_shouldThrowWhenStatusUnsupported() {
        assertThrows(ResponseStatusException.class, () -> categoryService.getCategories(1, "archived"));

        verify(categoryRepository, never()).findByCompanyId(1);
        verify(categoryRepository, never()).findByCompanyIdAndIsDeletedFalse(1);
        verify(categoryRepository, never()).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getCategoryById_shouldReturnCategoryWhenFound() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1))
                .thenReturn(Optional.of(category));

        Optional<CategoryResponse> result = categoryService.getCategoryById(4, 1);

        assertTrue(result.isPresent());
        assertEquals(4, result.get().categoryId());
        assertEquals("Floor", result.get().description());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1);
    }

    @Test
    void getCategoryById_shouldReturnEmptyWhenNotFound() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1))
                .thenReturn(Optional.empty());

        Optional<CategoryResponse> result = categoryService.getCategoryById(4, 1);

        assertFalse(result.isPresent());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1);
    }

    @Test
    void createCategory_shouldSaveCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(1, "FLR", "Floor", "08:00", "17:00", 12, (short) 3);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.createCategory(request);

        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    void updateCategory_shouldUpdateExistingCategory() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("FLOOR", "Updated Floor", "09:00", "18:00", 12, (short) 4);
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.updateCategory(4, 1, request);

        assertEquals("Updated Floor", category.getDescription());
        assertEquals("FLOOR", category.getShortDesc());
        assertEquals("09:00", category.getStartTime());
        assertEquals("18:00", category.getEndTime());
        assertEquals(12, category.getSkillId());
        assertEquals((short) 4, category.getColor());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1);
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_shouldThrowWhenNotFound() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("FLOOR", "Updated Floor", "09:00", "18:00", 12, (short) 4);
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoryService.updateCategory(4, 1, request));
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_shouldSoftDeleteCategory() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.deleteCategory(4, 1);

        assertTrue(category.getIsDeleted());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1);
        verify(categoryRepository).save(category);
    }

    @Test
    void deleteCategory_shouldThrowWhenNotFound() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(4, 1));
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(4, 1);
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
