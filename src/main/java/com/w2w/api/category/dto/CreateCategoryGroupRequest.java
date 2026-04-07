package com.w2w.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCategoryGroupRequest(
        @NotNull(message = "Company ID cannot be null")
        Integer companyId,

        @NotBlank(message = "Description cannot be blank")
        String description,

        List<Integer> categoryIds
) {
}
