package com.w2w.api.category.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
        @NotBlank(message = "Short description cannot be blank")
        String shortDesc,

        @NotBlank(message = "Description cannot be blank")
        String description,

        String startTime,

        String endTime,

        Integer positionId,

        Short color
) {
}
