package com.w2w.api.scheduling.dto;

import java.util.List;

public record PositionTimingBucket(
        String position,
        List<ShiftTimingBucket> shiftTimings,
        Integer shiftCount,
        Float totalDuration
) {
}
