package com.w2w.api.scheduling.dto;

import java.util.List;

public record FindConflictRequest(
        Integer companyId,
        String operationType,
        UpdateShiftRequest shift
) {
}
