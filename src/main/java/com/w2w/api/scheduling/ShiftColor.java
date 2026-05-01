package com.w2w.api.scheduling;

import java.util.Arrays;
import java.util.List;

public enum ShiftColor {
    BLACK((short) 0, "black"),
    AMBER((short) 1, "amber"),
    BLUE((short) 2, "blue"),
    CHARCOAL((short) 3, "charcoal"),
    GRAY((short) 4, "gray");

    private static final String DEFAULT_COLOR = BLACK.color;

    private final Short id;
    private final String color;

    ShiftColor(Short id, String color) {
        this.id = id;
        this.color = color;
    }

    public Short id() {
        return id;
    }

    public String color() {
        return color;
    }

    public static String colorForId(Short id) {
        return Arrays.stream(values())
                .filter(value -> value.id.equals(id))
                .findFirst()
                .map(ShiftColor::color)
                .orElse(DEFAULT_COLOR);
    }

    public static List<ShiftColor> palette() {
        return List.of(values());
    }
}
