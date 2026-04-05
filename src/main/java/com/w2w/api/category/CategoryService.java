package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryResponse;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.dto.CreateCategoryRequest;
import com.w2w.api.category.dto.UpdateCategoryRequest;
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
        throw new UnsupportedOperationException("Category get by id is not implemented yet");
    }

    @Transactional
    public void createCategory(CreateCategoryRequest request) {
        throw new UnsupportedOperationException("Category create is not implemented yet");
    }

    @Transactional
    public void updateCategory(Integer categoryId, Integer companyId, UpdateCategoryRequest request) {
        throw new UnsupportedOperationException("Category update is not implemented yet");
    }

    @Transactional
    public void deleteCategory(Integer categoryId, Integer companyId) {
        throw new UnsupportedOperationException("Category delete is not implemented yet");
    }

    private CategorySummary toSummary(com.w2w.api.category.model.Category category) {
        return new CategorySummary(category.getCategoryId(), category.getDescription(), category.getShortDesc());
    }
}
