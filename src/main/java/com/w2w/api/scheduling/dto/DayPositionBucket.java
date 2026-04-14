package com.w2w.api.scheduling.dto;

import java.math.BigDecimal;
import java.util.List;

public record DayPositionBucket(
        String weekday,
        String date,
        List<PositionShiftBucket> positions,
        Integer shiftCount,
        BigDecimal totalDuration
) {
}
