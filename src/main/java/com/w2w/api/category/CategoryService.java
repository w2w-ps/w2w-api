package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryDto;
import com.w2w.api.category.dto.CategoryGroupDto;
import com.w2w.api.category.repository.CategoryGroupRepository;
import com.w2w.api.category.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryGroupRepository categoryGroupRepository;

    public List<CategoryDto> getCategoriesByCompanyId(Integer companyId) {
        return categoryRepository.findByCompanyId(companyId).stream()
                .map(cat -> new CategoryDto(cat.getCategoryId(), cat.getDescription(), cat.getShortDesc()))
                .collect(Collectors.toList());
    }

    public List<CategoryGroupDto> getCategoryGroupsByCompanyId(Integer companyId) {
        return categoryGroupRepository.findByCompanyId(companyId).stream()
                .map(group -> new CategoryGroupDto(
                        group.getGroupId(),
                        group.getDescription(),
                        group.getCategories().stream()
                                .map(cat -> new CategoryDto(cat.getCategoryId(), cat.getDescription(), cat.getShortDesc()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
