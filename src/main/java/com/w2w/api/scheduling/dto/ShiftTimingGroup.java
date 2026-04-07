package com.w2w.api.scheduling.dto;

import java.util.List;

public record ShiftTimingGroup(
        String label,
        List<EmployeeScheduledShift> shifts
) {
}
