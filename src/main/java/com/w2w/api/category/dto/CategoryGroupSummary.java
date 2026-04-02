package com.w2w.api.category.dto;

import java.util.List;

public record CategoryGroupSummary(Integer id, String name, List<CategorySummary> categories) {
}
