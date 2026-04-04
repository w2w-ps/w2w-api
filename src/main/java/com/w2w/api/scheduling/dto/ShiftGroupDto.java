package com.w2w.api.scheduling.dto;

import java.util.List;

public record ShiftGroupDto(
        String label,
        List<ShiftGroupDto> shiftGroups,
        List<EmployeeScheduledShiftDto> shifts
) {
}
