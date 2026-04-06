package com.w2w.api.scheduling.dto;

import java.util.List;

public record PositionShiftBucket(
        String position,
        List<EmployeeShift> shifts,
        Integer shiftCount,
        Float totalDuration
) {
}
