package com.w2w.api.timeoff.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TimeOffRequestSummary(
        Integer requestId,
        Integer employeeId,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        String endDateTimes,
        LocalDateTime requestedAt,
        String status,
        String comments,
        String repeatSummary,
        Boolean canCancel
) {
}