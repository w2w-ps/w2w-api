package com.w2w.api.scheduling.dto;

import java.util.List;

public record CategoryTimingBucketDto(
        String category,
        List<ShiftTimingGroupDto> shiftTimings
) {
}
