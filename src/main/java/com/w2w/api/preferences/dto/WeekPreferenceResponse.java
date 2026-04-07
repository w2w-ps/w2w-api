package com.w2w.api.preferences.dto;

import java.time.LocalDate;

public record WeekPreferenceResponse(
    LocalDate startDate,
    String prefs
) {}
