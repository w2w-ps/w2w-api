package com.w2w.api.category;

import com.w2w.api.category.dto.*;
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
            @RequestParam(defaultValue = "all") String status
    ) {
        List<CategorySummary> categories = categoryService.getCategories(status);
        return ResponseEntity.ok(new CategoriesResponse(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable("id") Integer categoryId) {
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId));
    }

    @PostMapping
    public ResponseEntity<Void> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        categoryService.createCategory(
                request.shortName(),
                request.description(),
                request.startTime(),
                request.endTime(),
                request.positionId(),
                request.color()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategory(
            @PathVariable("id") Integer categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        categoryService.updateCategory(categoryId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable("id") Integer categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
