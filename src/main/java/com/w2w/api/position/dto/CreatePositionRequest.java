package com.w2w.api.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePositionRequest(
    @NotNull(message = "Company ID cannot be null")
    Integer companyId,

    @NotBlank(message = "Description cannot be blank")
    String description
) {
}