package com.w2w.api.preferences.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record WeekPreferenceRequest(
    @NotNull @Positive Integer employeeId,
    @NotNull @Positive Integer companyId,
    @NotNull LocalDate startDate,
    @Size(max = 672) String prefs,
    Integer compression,
    @Positive Integer editedBy
) {}
