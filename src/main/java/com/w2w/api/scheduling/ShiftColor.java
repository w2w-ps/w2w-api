package com.w2w.api.scheduling;

import java.util.Arrays;
import java.util.List;

public enum ShiftColor {
    BLACK((short) 0, "black"),
    BROWN((short) 1, "brown"),
    BLUE((short) 2, "blue"),
    FUCHSIA((short) 3, "fuchsia"),
    GRAY((short) 4, "gray"),
    GREEN((short) 5, "green"),
    NAVY((short) 6, "navy"),
    ORANGE((short) 7, "orange"),
    PURPLE((short) 8, "purple"),
    RED((short) 9, "red"),
    TURQUOISE((short) 10, "turquoise"),
    LAVENDER((short) 11, "lavender"),
    LIME((short) 12, "lime"),
    SALMON((short) 13, "salmon"),
    GOLD((short) 14, "gold"),
    AQUA((short) 15, "aqua"),
    MAROON((short) 16, "maroon");

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
