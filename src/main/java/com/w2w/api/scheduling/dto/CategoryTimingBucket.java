package com.w2w.api.scheduling.dto;

import java.util.List;

public record CategoryTimingBucket(
        String category,
        List<ShiftTimingGroup> shiftTimings
) {
}
