package com.w2w.api.scheduling.dto;

import java.util.List;

public record GroupedShiftsResponseDto(
        List<GroupedShiftDateDto> dates
) {
}
