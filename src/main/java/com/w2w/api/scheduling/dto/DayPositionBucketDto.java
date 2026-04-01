package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record DayPositionBucketDto(
        LocalDate date,
        List<PositionShiftBucketDto> positions,
        Integer shiftCount,
        Float totalDuration
) {
}
