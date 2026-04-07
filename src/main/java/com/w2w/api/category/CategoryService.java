package com.w2w.api.category;

import com.w2w.api.category.dto.*;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryGroupRepository;
import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryGroupRepository categoryGroupRepository;

    public CategoryService(CategoryRepository categoryRepository, CategoryGroupRepository categoryGroupRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryGroupRepository = categoryGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> getCategories(String status) {
        Integer currentTenant = TenantContext.getCurrentTenant();
        return switch (status.toLowerCase()) {
            case "all" -> categoryRepository.findByCompanyId(currentTenant).stream()
                    .map(this::toSummary)
                    .toList();
            case "active" -> categoryRepository.findByCompanyIdAndIsDeletedFalse(currentTenant).stream()
                    .map(this::toSummary)
                    .toList();
            case "inactive" -> categoryRepository.findByCompanyIdAndIsDeletedTrue(currentTenant).stream()
                    .map(this::toSummary)
                    .toList();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter");
        };
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> getCategoriesByCompanyId() {
        return getCategories("all");
    }

    @Transactional(readOnly = true)
    public List<CategoryGroupSummary> getCategoryGroupsByCompanyId() {
        return categoryGroupRepository.findByCompanyId(TenantContext.getCurrentTenant()).stream()
                .map(group -> new CategoryGroupSummary(
                        group.getGroupId(),
                        group.getDescription(),
                        group.getCategories().stream()
                                .map(cat -> new CategorySummary(cat.getCategoryId(), cat.getDescription(), cat.getShortDesc()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponse> getCategoryById(Integer categoryId) {
        return categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(
                        categoryId,
                        TenantContext.getCurrentTenant()
                )
                .map(this::toResponse);
    }

    @Transactional
    public void createCategory(
            String shortName,
            String description,
            String startTime,
            String endTime,
            Integer positionId,
            Short color
    ) {
        Category category = new Category();
        category.setCompanyId(CurrentTenant.requireCurrentTenant());
        category.setShortDesc(shortName);
        category.setDescription(description);
        category.setStartTime(startTime);
        category.setEndTime(endTime);
        category.setPositionId(positionId);
        category.setColor(color);
        category.setIsDeleted(false);

        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategory(Integer categoryId, UpdateCategoryRequest request) {
        Category category = requireActiveCategory(categoryId, CurrentTenant.requireCurrentTenant());
        category.setShortDesc(request.shortName());
        category.setDescription(request.description());
        category.setStartTime(request.startTime());
        category.setEndTime(request.endTime());
        category.setPositionId(request.positionId());
        category.setColor(request.color());

        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Integer categoryId) {
        Category category = requireActiveCategory(categoryId, CurrentTenant.requireCurrentTenant());
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    private CategorySummary toSummary(Category category) {
        return new CategorySummary(category.getCategoryId(), category.getDescription(), category.getShortDesc());
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getDescription(),
                category.getShortDesc(),
                category.getStartTime(),
                category.getEndTime(),
                category.getPositionId(),
                category.getColor()
        );
    }

    private Category requireActiveCategory(Integer categoryId, Integer companyId) {
        return categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(categoryId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}
