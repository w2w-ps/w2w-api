package com.w2w.api.scheduling.dto;

import java.util.List;

public record PositionTimingBucketDto(
        String position,
        List<ShiftTimingBucketDto> shiftTimings,
        Integer shiftCount,
        Float totalDuration
) {
}
