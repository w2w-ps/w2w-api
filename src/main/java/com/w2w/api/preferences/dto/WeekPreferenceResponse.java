package com.w2w.api.preferences.dto;

import java.time.LocalDate;

public record WeekPreferenceResponse(
    Integer employeeId,
    Integer companyId,
    LocalDate startDate,
    String prefs,
    Integer compression,
    Integer editedBy
) {}
