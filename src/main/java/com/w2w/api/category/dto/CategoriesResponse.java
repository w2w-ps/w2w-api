package com.w2w.api.category.dto;

import java.util.List;

public class CategoriesResponse {
    private List<CategoryDto> categories;
    private List<CategoryGroupDto> groups;

    public CategoriesResponse() {}

    public CategoriesResponse(List<CategoryDto> categories, List<CategoryGroupDto> groups) {
        this.categories = categories;
        this.groups = groups;
    }

    public List<CategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
    }

    public List<CategoryGroupDto> getGroups() {
        return groups;
    }

    public void setGroups(List<CategoryGroupDto> groups) {
        this.groups = groups;
    }
}
