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
    public ResponseEntity<CategoriesResponse> get(
            @RequestParam(defaultValue = "active") String status
    ) {
        List<CategoryResponse> categories = categoryService.get(status);
        return ResponseEntity.ok(new CategoriesResponse(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> get(@PathVariable("id") Integer categoryId) {
        return ResponseEntity.ok(categoryService.get(categoryId));
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CreateCategoryRequest request) {
        categoryService.create(
                request.shortDesc(),
                request.description(),
                request.startTime(),
                request.endTime(),
                request.positionId(),
                request.color()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable("id") Integer categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        categoryService.update(categoryId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.noContent().build();
    }
}
