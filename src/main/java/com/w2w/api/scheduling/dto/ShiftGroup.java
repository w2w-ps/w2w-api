package com.w2w.api.scheduling.dto;

import java.util.List;

public record ShiftGroup(
        String label,
        List<ShiftGroup> shiftGroups,
        List<EmployeeScheduledShift> shifts
) {
}
