package com.w2w.api.preferences.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DayPreferenceListRequest(
    @NotNull List<@Valid @NotNull DayPreferenceRequest> preferences
) {}
