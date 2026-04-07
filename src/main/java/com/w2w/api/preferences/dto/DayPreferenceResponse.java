package com.w2w.api.preferences.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public record DayPreferenceResponse(
    LocalDate date,
    String prefs,
    String day,
    Boolean isDayPrefs
) {
    public DayPreferenceResponse(
            LocalDate date,
            String prefs,
            Boolean isDayPrefs
    ) {
        this(date, prefs,
                date != null ? date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) : null,
                isDayPrefs);
    }
}
