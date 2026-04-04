package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record DayShiftTimingBucketDto(
        LocalDate date,
        List<ShiftTimingGroupDto> shiftTimings
) {
}
