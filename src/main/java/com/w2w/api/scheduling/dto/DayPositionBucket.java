package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record DayPositionBucket(
        LocalDate date,
        List<PositionShiftBucket> positions,
        Integer shiftCount,
        Float totalDuration
) {
}
