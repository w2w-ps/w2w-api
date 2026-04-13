package com.w2w.api.categorygroup;

import com.w2w.api.categorygroup.dto.CategoryGroupSummary;
import com.w2w.api.categorygroup.dto.CategoryGroupsResponse;
import com.w2w.api.categorygroup.dto.CreateCategoryGroupRequest;
import com.w2w.api.categorygroup.dto.UpdateCategoryGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes tenant-scoped category-group CRUD endpoints.
 */
@RestController
@RequestMapping("/api/category-groups")
public class CategoryGroupController {

    private final CategoryGroupService categoryGroupService;

    public CategoryGroupController(CategoryGroupService categoryGroupService) {
        this.categoryGroupService = categoryGroupService;
    }

    /**
     * Returns the category groups for the authenticated tenant.
     */
    @GetMapping
    public ResponseEntity<CategoryGroupsResponse> getCategoryGroups(
            @RequestParam(defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(new CategoryGroupsResponse(categoryGroupService.getCategoryGroups(status)));
    }

    /**
     * Returns a single category group when it exists for the authenticated tenant.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryGroupSummary> getCategoryGroupById(
            @PathVariable("id") Integer groupId
    ) {
        return categoryGroupService.getCategoryGroupById(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a category group for the authenticated tenant.
     */
    @PostMapping
    public ResponseEntity<Void> createCategoryGroup(@Valid @RequestBody CreateCategoryGroupRequest request) {
        categoryGroupService.createCategoryGroup(request.description(), request.categoryIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates an existing category group and its category membership.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateCategoryGroup(
            @PathVariable("id") Integer groupId,
            @Valid @RequestBody UpdateCategoryGroupRequest request
    ) {
        categoryGroupService.updateCategoryGroup(groupId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a category group and its join-table memberships.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategoryGroup(@PathVariable("id") Integer groupId) {
        categoryGroupService.deleteCategoryGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
