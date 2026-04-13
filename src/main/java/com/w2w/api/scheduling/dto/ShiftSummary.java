package com.w2w.api.scheduling.dto;

public record ShiftSummary(
        Integer shiftId,
        String startTime,
        String endTime,
        String position,
        String category,
        String description,
        Float duration,
        String color
) {
}
