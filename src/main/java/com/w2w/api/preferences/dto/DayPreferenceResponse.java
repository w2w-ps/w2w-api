package com.w2w.api.preferences.dto;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public record DayPreferenceResponse(
    Integer employeeId,
    Integer companyId,
    LocalDate date,
    String prefs,
    Integer compression,
    Integer editedBy,
    String day,
    Boolean isDayPrefs
) {
    public DayPreferenceResponse(
            Integer employeeId,
            Integer companyId,
            LocalDate date,
            String prefs,
            Integer compression,
            Integer editedBy,
            Boolean isDayPrefs
    ) {
        this(employeeId, companyId, date, prefs, compression, editedBy,
                date != null ? date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) : null,
                isDayPrefs);
    }
}
