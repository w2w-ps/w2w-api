package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryResponse;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.dto.CreateCategoryRequest;
import com.w2w.api.category.dto.UpdateCategoryRequest;
import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> getCategories(Integer companyId, String status) {
        return switch (status.toLowerCase()) {
            case "all" -> categoryRepository.findByCompanyId(companyId).stream()
                    .map(this::toSummary)
                    .toList();
            case "active" -> categoryRepository.findByCompanyIdAndIsDeletedFalse(companyId).stream()
                    .map(this::toSummary)
                    .toList();
            case "inactive" -> categoryRepository.findByCompanyIdAndIsDeletedTrue(companyId).stream()
                    .map(this::toSummary)
                    .toList();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter");
        };
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> getCategoriesByCompanyId(Integer companyId) {
        return getCategories(companyId, "all");
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponse> getCategoryById(Integer categoryId, Integer companyId) {
        return categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(categoryId, companyId)
                .map(this::toResponse);
    }

    @Transactional
    public void createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setCompanyId(request.companyId());
        category.setShortDesc(request.shortName());
        category.setDescription(request.description());
        category.setStartTime(request.startTime());
        category.setEndTime(request.endTime());
        category.setSkillId(request.skillId());
        category.setColor(request.color());
        category.setIsDeleted(false);

        categoryRepository.save(category);
    }

    @Transactional
    public void updateCategory(Integer categoryId, Integer companyId, UpdateCategoryRequest request) {
        Category category = requireActiveCategory(categoryId, companyId);
        category.setShortDesc(request.shortName());
        category.setDescription(request.description());
        category.setStartTime(request.startTime());
        category.setEndTime(request.endTime());
        category.setSkillId(request.skillId());
        category.setColor(request.color());

        categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Integer categoryId, Integer companyId) {
        Category category = requireActiveCategory(categoryId, companyId);
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
                category.getSkillId(),
                category.getColor()
        );
    }

    private Category requireActiveCategory(Integer categoryId, Integer companyId) {
        return categoryRepository.findByCategoryIdAndCompanyIdAndIsDeletedFalse(categoryId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}
