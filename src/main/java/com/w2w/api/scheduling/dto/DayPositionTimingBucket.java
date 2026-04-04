package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record DayPositionTimingBucket(
        LocalDate date,
        List<PositionTimingBucket> positions,
        Integer shiftCount,
        Float totalDuration
) {
}
