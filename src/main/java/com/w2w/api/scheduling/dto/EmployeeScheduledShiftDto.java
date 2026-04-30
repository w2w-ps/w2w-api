package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;

public record EmployeeScheduledShiftDto(
        Integer shiftId,
        Integer employeeId,
        String firstName,
        String lastName,
        List<String> phones,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime endTime,
        String position,
        String category,
        String description,
        Float duration,
        String color
) {
}
