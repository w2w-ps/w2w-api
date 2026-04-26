package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictItem;

import java.util.List;

interface SchedulingValidationRule {
    List<ConflictItem> validate(SchedulingValidationContext context);
}
