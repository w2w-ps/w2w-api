package com.w2w.api.category.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        String shortName,

        @NotBlank(message = "Description cannot be blank")
        String description,

        String startTime,

        String endTime,

        Integer skillId,

        Short color
) {
}
