package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record DayPositionTimingBucketDto(
        LocalDate date,
        List<PositionTimingBucketDto> positions,
        Integer shiftCount,
        Float totalDuration
) {
}
