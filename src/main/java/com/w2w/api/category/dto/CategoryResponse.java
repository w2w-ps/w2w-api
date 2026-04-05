package com.w2w.api.category.dto;

public record CategoryResponse(
        Integer categoryId,
        String description,
        String shortName,
        String startTime,
        String endTime,
        Integer skillId,
        Short color
) {
}
