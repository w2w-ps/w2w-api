package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryGroupSummary;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.dto.CreateCategoryGroupRequest;
import com.w2w.api.category.dto.UpdateCategoryGroupRequest;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.model.CategoryGroup;
import com.w2w.api.category.repository.CategoryGroupRepository;
import com.w2w.api.category.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coordinates category-group persistence and category membership validation.
 */
@Service
public class CategoryGroupService {

    private final CategoryGroupRepository categoryGroupRepository;
    private final CategoryRepository categoryRepository;

    public CategoryGroupService(CategoryGroupRepository categoryGroupRepository, CategoryRepository categoryRepository) {
        this.categoryGroupRepository = categoryGroupRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns the category groups for a company filtered by status.
     */
    @Transactional(readOnly = true)
    public List<CategoryGroupSummary> getCategoryGroups(Integer companyId, String status) {
        return findCategoryGroupsByStatus(companyId, status).stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Returns a single category group when it exists for the company.
     */
    @Transactional(readOnly = true)
    public Optional<CategoryGroupSummary> getCategoryGroupById(Integer groupId, Integer companyId) {
        return categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .map(this::toSummary);
    }

    /**
     * Creates a category group with the requested category membership.
     */
    @Transactional
    public void createCategoryGroup(CreateCategoryGroupRequest request) {
        CategoryGroup categoryGroup = new CategoryGroup();
        categoryGroup.setCompanyId(request.companyId());
        categoryGroup.setDescription(request.description());
        categoryGroup.setIsDeleted(false);
        categoryGroup.setCategories(resolveCategories(request.categoryIds(), request.companyId()));
        categoryGroupRepository.save(categoryGroup);
    }

    /**
     * Updates a category group and replaces its category membership.
     */
    @Transactional
    public void updateCategoryGroup(Integer groupId, Integer companyId, UpdateCategoryGroupRequest request) {
        CategoryGroup categoryGroup = requireActiveCategoryGroup(groupId, companyId);
        categoryGroup.setDescription(request.description());
        categoryGroup.setCategories(resolveCategories(request.categoryIds(), companyId));
        categoryGroupRepository.save(categoryGroup);
    }

    /**
     * Soft-deletes a category group.
     */
    @Transactional
    public void deleteCategoryGroup(Integer groupId, Integer companyId) {
        CategoryGroup categoryGroup = requireActiveCategoryGroup(groupId, companyId);
        categoryGroup.setIsDeleted(true);
        categoryGroupRepository.save(categoryGroup);
    }

    /**
     * Maps the persisted category group to the API summary shape.
     */
    private CategoryGroupSummary toSummary(CategoryGroup categoryGroup) {
        return new CategoryGroupSummary(
                categoryGroup.getGroupId(),
                categoryGroup.getDescription(),
                categoryGroup.getCategories().stream()
                        .filter(category -> !Boolean.TRUE.equals(category.getIsDeleted()))
                        .map(this::toCategorySummary)
                        .toList()
        );
    }

    /**
     * Maps a category entity to the nested category-group response shape.
     */
    private CategorySummary toCategorySummary(Category category) {
        return new CategorySummary(category.getCategoryId(), category.getDescription(), category.getShortDesc());
    }

    /**
     * Loads an active category group for the requested tenant or raises a 404.
     */
    private CategoryGroup requireActiveCategoryGroup(Integer groupId, Integer companyId) {
        return categoryGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category group not found"));
    }

    /**
     * Finds category groups for the requested tenant and status filter.
     */
    private List<CategoryGroup> findCategoryGroupsByStatus(Integer companyId, String status) {
        return switch (status.toLowerCase()) {
            case "all" -> categoryGroupRepository.findByCompanyId(companyId);
            case "active" -> categoryGroupRepository.findByCompanyIdAndIsDeletedFalse(companyId);
            case "non-active" -> categoryGroupRepository.findByCompanyIdAndIsDeletedTrue(companyId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter");
        };
    }

    /**
     * Resolves the requested category IDs to active tenant-scoped category entities.
     */
    private List<Category> resolveCategories(Collection<Integer> requestedCategoryIds, Integer companyId) {
        LinkedHashSet<Integer> uniqueCategoryIds = new LinkedHashSet<>(requestedCategoryIds);
        if (uniqueCategoryIds.isEmpty()) {
            return List.of();
        }

        List<Category> categories = categoryRepository.findByCategoryIdInAndCompanyIdAndIsDeletedFalse(uniqueCategoryIds, companyId);
        if (categories.size() != uniqueCategoryIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more categories were not found for the company"
            );
        }

        Map<Integer, Category> categoriesById = categories.stream()
                .collect(Collectors.toMap(Category::getCategoryId, Function.identity()));

        return uniqueCategoryIds.stream()
                .map(categoriesById::get)
                .toList();
    }
}
