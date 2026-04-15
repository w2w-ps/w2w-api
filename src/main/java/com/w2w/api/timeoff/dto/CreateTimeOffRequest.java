package com.w2w.api.timeoff.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateTimeOffRequest(
        @NotNull Integer companyId,
        @NotNull Integer employeeId,
        @NotNull LocalDate date,
        @NotNull Boolean fullDay,
        Integer dayCount,
        LocalTime startTime,
        LocalTime endTime,
        @Min(1) Integer repeatCount,
        String comments
) {
}