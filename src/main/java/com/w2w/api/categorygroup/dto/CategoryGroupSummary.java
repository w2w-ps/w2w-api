package com.w2w.api.categorygroup.dto;

import com.w2w.api.category.dto.CategorySummary;

import java.util.List;

public record CategoryGroupSummary(Integer id, String name, List<CategorySummary> categories) {
}
