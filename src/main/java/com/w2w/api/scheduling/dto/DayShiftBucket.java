package com.w2w.api.scheduling.dto;

import java.util.List;

public record DayShiftBucket(String date, List<ShiftSummary> shifts) {
}
