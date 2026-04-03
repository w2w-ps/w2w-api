package com.w2w.api.scheduling.dto;

import java.util.List;

public record PositionShiftBucketDto(
        String position,
        List<EmployeeScheduledShiftDto> shifts,
        Integer shiftCount,
        Float totalDuration
) {
}
