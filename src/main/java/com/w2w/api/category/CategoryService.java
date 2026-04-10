package com.w2w.api.category;

import com.w2w.api.category.dto.*;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> getCategories(String status) {
        Integer currentTenant = CurrentTenant.requireCurrentTenant();
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
    public CategoryResponse getCategoryById(Integer categoryId) {
        return toResponse(requireActiveCategory(categoryId, CurrentTenant.requireCurrentTenant()));
    }

    @Transactional
    @PreAuthorize("@categoryPolicy.canManage(authentication)")
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
    @PreAuthorize("@categoryPolicy.canManage(authentication)")
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
    @PreAuthorize("@categoryPolicy.canManage(authentication)")
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
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
