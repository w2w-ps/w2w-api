package com.w2w.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(
        @NotNull(message = "Company ID cannot be null")
        Integer companyId,

        String shortName,

        @NotBlank(message = "Description cannot be blank")
        String description,

        String startTime,

        String endTime,

        Integer positionId,

        Short color
) {
}
