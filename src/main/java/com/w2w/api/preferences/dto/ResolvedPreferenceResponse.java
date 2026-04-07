package com.w2w.api.preferences.dto;

import java.time.LocalDate;

public record ResolvedPreferenceResponse(
    LocalDate date,
    String prefs,
    String preferenceType,
    String day
) {}
