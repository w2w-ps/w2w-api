package com.w2w.api.position.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePositionRequest(
    @NotBlank(message = "Description cannot be blank")
    String description
) {
}