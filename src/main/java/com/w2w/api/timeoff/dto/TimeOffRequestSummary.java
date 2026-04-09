package com.w2w.api.timeoff.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TimeOffRequestSummary(
        Integer requestId,
        Integer employeeId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate endDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
        LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
        LocalTime endTime,
        String endDateTimes,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime requestedAt,
        String status,
        String comments,
        String repeatSummary,
        Boolean canCancel
) {
}