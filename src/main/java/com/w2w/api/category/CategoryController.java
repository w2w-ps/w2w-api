package com.w2w.api.category;

import com.w2w.api.category.dto.CategoriesResponse;
import com.w2w.api.category.dto.CategoryResponse;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.dto.CreateCategoryRequest;
import com.w2w.api.category.dto.UpdateCategoryRequest;
import com.w2w.api.config.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<CategoriesResponse> getCategories(
            @RequestParam Integer companyId,
            @RequestParam(defaultValue = "all") String status
    ) {
        TenantContext.setCurrentTenant(companyId);
        List<CategorySummary> categories = categoryService.getCategories(companyId, status);
        return ResponseEntity.ok(new CategoriesResponse(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("id") Integer categoryId, @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        return categoryService.getCategoryById(categoryId, companyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        categoryService.createCategory(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable("id") Integer categoryId,
            @RequestParam Integer companyId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        TenantContext.setCurrentTenant(companyId);
        categoryService.updateCategory(categoryId, companyId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Integer categoryId, @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        categoryService.deleteCategory(categoryId, companyId);
        return ResponseEntity.noContent().build();
    }
}
