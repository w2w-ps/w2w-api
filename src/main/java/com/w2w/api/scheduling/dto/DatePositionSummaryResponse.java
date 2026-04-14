package com.w2w.api.scheduling.dto;

import java.math.BigDecimal;
import java.util.List;

public record DatePositionSummaryResponse(
        String title,
        Integer totalShifts,
        BigDecimal totalHours,
        List<DayPositionBucket> dates
) {
}
