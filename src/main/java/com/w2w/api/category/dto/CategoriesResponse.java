package com.w2w.api.category.dto;

import java.util.List;

public record CategoriesResponse(
        List<CategorySummary> categories,
        List<CategoryGroupSummary> categoryGroups
) {
}
