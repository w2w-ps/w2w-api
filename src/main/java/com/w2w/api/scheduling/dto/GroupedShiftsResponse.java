package com.w2w.api.scheduling.dto;

import java.util.List;

public record GroupedShiftsResponse(
        List<GroupedShiftDate> dates
) {
}
