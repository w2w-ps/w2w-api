package com.w2w.api.scheduling.dto;

import java.util.Arrays;

public enum ShiftGrouping {
    POSITION_SHIFT_TIMINGS("position_shift_timings"),
    SHIFT_TIMINGS("shift_timings"),
    CATEGORY_SHIFT_TIMINGS("category_shift_timings"),
    CAT_SHIFT_TIMINGS("cat_shift_timings");

    private final String value;

    ShiftGrouping(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ShiftGrouping fromString(String value) {
        return Arrays.stream(ShiftGrouping.values())
                .filter(g -> g.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown grouping: " + value));
    }
}
