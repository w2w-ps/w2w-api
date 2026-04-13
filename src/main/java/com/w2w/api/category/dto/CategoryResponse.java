package com.w2w.api.category.dto;

public record CategoryResponse(
        Integer categoryId,
        String description,
        String shortDesc,
        String startTime,
        String endTime,
        Integer positionId,
        Short color
) {
}
