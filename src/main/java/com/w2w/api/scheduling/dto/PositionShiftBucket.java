package com.w2w.api.scheduling.dto;

import java.math.BigDecimal;
import java.util.List;

public record PositionShiftBucket(
        String position,
        List<EmployeeShift> shifts,
        Integer shiftCount,
        BigDecimal totalDuration
) {
}
