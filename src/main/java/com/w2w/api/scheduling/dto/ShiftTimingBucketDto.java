package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;
import java.util.List;

public record ShiftTimingBucketDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime endTime,
        List<EmployeeScheduledShiftDto> shifts,
        Integer shiftCount,
        Float totalDuration
) {
}
