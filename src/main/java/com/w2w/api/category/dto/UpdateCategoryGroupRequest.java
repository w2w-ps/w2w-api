package com.w2w.api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateCategoryGroupRequest(
        @NotBlank(message = "Description cannot be blank")
        String description,

        @NotNull(message = "Category IDs cannot be null")
        List<@NotNull(message = "Category ID cannot be null") Integer> categoryIds
) {
}
