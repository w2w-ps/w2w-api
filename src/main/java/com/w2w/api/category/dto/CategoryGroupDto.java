package com.w2w.api.category.dto;

import java.util.List;

public class CategoryGroupDto {
    private Integer id;
    private String name;
    private List<CategoryDto> categories;

    public CategoryGroupDto() {}

    public CategoryGroupDto(Integer id, String name, List<CategoryDto> categories) {
        this.id = id;
        this.name = name;
        this.categories = categories;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CategoryDto> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDto> categories) {
        this.categories = categories;
    }
}
