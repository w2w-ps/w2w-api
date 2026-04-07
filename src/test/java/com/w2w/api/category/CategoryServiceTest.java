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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Integer companyId;
    private Category category;
    private CategoryResponse categoryResponse;
    private CategorySummary categorySummary;

    @BeforeEach
    void setUp() {
        companyId = 1;
        category = new Category();
        category.setCategoryId(1);
        category.setCompanyId(companyId);
        category.setDescription("Description");
        category.setShortDesc("ShortName");
        category.setStartTime("09:00");
        category.setEndTime("17:00");
        category.setPositionId(12);
        category.setColor((short) 1);
        category.setIsDeleted(false);

        categoryResponse = new CategoryResponse(1, "Description", "ShortName", "09:00", "17:00", 12, (short) 1);
        categorySummary = new CategorySummary(1, "Description", "ShortName");
    }

    @Test
    void getCategories_all_returnsAllCategories() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(2);
        deletedCategory.setIsDeleted(true);
        deletedCategory.setDescription("Deleted Category");

        when(categoryRepository.findByCompanyId(companyId)).thenReturn(List.of(category, deletedCategory));
        List<CategorySummary> result = categoryService.getCategories(companyId, "all");

        assertEquals(2, result.size());
        assertEquals("Description", result.get(0).description());
        assertEquals("Deleted Category", result.get(1).description());
        verify(categoryRepository).findByCompanyId(companyId);
    }

    @Test
    void getCategories_active_returnsActiveCategories() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(2);
        deletedCategory.setIsDeleted(true);
        deletedCategory.setDescription("Deleted Category");

        when(categoryRepository.findByCompanyIdAndIsDeletedFalse(companyId)).thenReturn(List.of(category));
        List<CategorySummary> result = categoryService.getCategories(companyId, "active");

        assertEquals(1, result.size());
        assertEquals("Description", result.get(0).description());
        verify(categoryRepository).findByCompanyIdAndIsDeletedFalse(companyId);
    }

    @Test
    void getCategories_inactive_returnsInactiveCategories() {
        Category deletedCategory = new Category();
        deletedCategory.setCategoryId(2);
        deletedCategory.setIsDeleted(true);
        deletedCategory.setDescription("Deleted Category");

        when(categoryRepository.findByCompanyIdAndIsDeletedTrue(companyId)).thenReturn(List.of(deletedCategory));
        List<CategorySummary> result = categoryService.getCategories(companyId, "inactive");

        assertEquals(1, result.size());
        assertEquals("Deleted Category", result.get(0).description());
        verify(categoryRepository).findByCompanyIdAndIsDeletedTrue(companyId);
    }

    @Test
    void getCategoriesByCompanyId_returnsAllCategories() {
        when(categoryRepository.findByCompanyId(companyId)).thenReturn(List.of(category));
        List<CategorySummary> result = categoryService.getCategoriesByCompanyId(companyId);

        assertEquals(1, result.size());
        assertEquals("Description", result.get(0).description());
        verify(categoryRepository).findByCompanyId(companyId);
    }

    @Test
    void getCategoryById_returnsCategoryResponse() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId)).thenReturn(Optional.of(category));

        Optional<CategoryResponse> result = categoryService.getCategoryById(1, companyId);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().categoryId());
        assertEquals("Description", result.get().description());
        assertEquals("ShortName", result.get().shortName());
        assertEquals("09:00", result.get().startTime());
        assertEquals("17:00", result.get().endTime());
        assertEquals(12, result.get().positionId());
        assertEquals((short) 1, result.get().color());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId);
    }

    @Test
    void getCategoryById_notFound_returnsEmptyOptional() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId)).thenReturn(Optional.empty());

        Optional<CategoryResponse> result = categoryService.getCategoryById(1, companyId);

        assertTrue(result.isEmpty());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId);
    }

    @Test
    void createCategory_savesNewCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest(companyId, "ShortName", "Description", "09:00", "17:00", 12, (short) 1);
        categoryService.createCategory(request);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void updateCategory_updatesAndSavesCategory() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("NewShortName", "New Description", "10:00", "18:00", 13, (short) 2);
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId)).thenReturn(Optional.of(category));

        categoryService.updateCategory(1, companyId, request);

        assertEquals("NewShortName", category.getShortDesc());
        assertEquals("New Description", category.getDescription());
        assertEquals("10:00", category.getStartTime());
        assertEquals("18:00", category.getEndTime());
        assertEquals(13, category.getPositionId());
        assertEquals((short) 2, category.getColor());
        verify(categoryRepository).save(category);
    }

    @Test
    void updateCategory_notFound_throwsNotFoundException() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("New ShortName", "New Description", "10:00", "18:00", 13, (short) 2);
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> categoryService.updateCategory(1, companyId, request));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_setsIsDeletedToTrue() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(1, companyId);

        assertTrue(category.getIsDeleted());
        verify(categoryRepository).save(category);
    }

    @Test
    void deleteCategory_notFound_throwsNotFoundException() {
        when(categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(1, companyId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(categoryRepository).findByCategoryIdAndCompanyIdAndIsDeletedFalse(1, companyId);
        verify(categoryRepository, never()).save(any(Category.class));
    }
}
