package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryGroupSummary;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.category.repository.CategoryGroupRepository;
import com.w2w.api.category.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryGroupRepository categoryGroupRepository;

    public List<CategorySummary> getCategoriesByCompanyId(Integer companyId) {
        return categoryRepository.findByCompanyId(companyId).stream()
                .map(cat -> new CategorySummary(cat.getCategoryId(), cat.getDescription(), cat.getShortDesc()))
                .collect(Collectors.toList());
    }

    public List<CategoryGroupSummary> getCategoryGroupsByCompanyId(Integer companyId) {
        return categoryGroupRepository.findByCompanyId(companyId).stream()
                .map(group -> new CategoryGroupSummary(
                        group.getGroupId(),
                        group.getDescription(),
                        group.getCategories().stream()
                                .map(cat -> new CategorySummary(cat.getCategoryId(), cat.getDescription(), cat.getShortDesc()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
