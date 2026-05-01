package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public record ShiftResponse(
        Integer shiftId,
        Integer employeeId,
        Integer companyId,
        String description,
        LocalDate date,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime endTime,
        Float duration,
        Boolean isOvernight,
        String position,
        String category,
        Short color
) {
}
