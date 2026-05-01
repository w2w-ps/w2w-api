package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateShiftRequest(
        Integer employeeId,
        Integer companyId,
        String description,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime startTime,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime endTime,
        Float duration,
        @NotNull Integer position,
        Integer category,
        Short color
) {
}
