package com.w2w.api.category.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cat_group")
public class CategoryGroup {
    @Id
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "description")
    private String description;

    @ManyToMany
    @JoinTable(
        name = "group_cat",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "cat_id")
    )
    private List<Category> categories = new ArrayList<>();

    public CategoryGroup() {}

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
