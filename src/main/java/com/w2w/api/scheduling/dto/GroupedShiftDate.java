package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record GroupedShiftDate(
        LocalDate date,
        List<ShiftGroup> shiftGroups
) {
}
