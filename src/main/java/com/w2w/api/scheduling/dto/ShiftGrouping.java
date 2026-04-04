package com.w2w.api.scheduling.dto;

import java.util.Arrays;

public enum ShiftGrouping {
    POSITION_SHIFT_TIMINGS("position_shift_timings"),
    SHIFT_TIMINGS("shift_timings"),
    CATEGORY_SHIFT_TIMINGS("category_shift_timings"),
    CAT_SHIFT_TIMINGS("cat_shift_timings");

    private final String apiValue;

    ShiftGrouping(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static ShiftGrouping fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(grouping -> grouping.apiValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported grouping: " + value));
    }
}
