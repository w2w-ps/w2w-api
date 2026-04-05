package com.w2w.api.positiongroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePositionGroupRequest(
        @NotNull(message = "Company ID cannot be null")
        Integer companyId,

        @NotBlank(message = "Description cannot be blank")
        String description,

        @NotNull(message = "Position IDs cannot be null")
        List<@NotNull(message = "Position ID cannot be null") Integer> positionIds
) {
}
