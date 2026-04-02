package com.w2w.api.category;

import com.w2w.api.category.dto.CategoriesResponse;
import com.w2w.api.category.dto.CategoryGroupSummary;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public CategoriesResponse getCategories(@RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        List<CategorySummary> categories = categoryService.getCategoriesByCompanyId(companyId);
        List<CategoryGroupSummary> groups = categoryService.getCategoryGroupsByCompanyId(companyId);
        return new CategoriesResponse(categories, groups);
    }
}
