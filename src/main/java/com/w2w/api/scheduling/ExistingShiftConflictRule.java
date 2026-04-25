package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(60)
class ExistingShiftConflictRule implements SchedulingValidationRule {
    private static final String FIELD = "shift";
    private static final String MESSAGE = "Overlaps existing shift";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasTimedShift()) {
            return List.of();
        }

        ShiftInterval proposedInterval = context.proposedInterval();
        if (proposedInterval == null) {
            return List.of();
        }

        for (DailyHoursShiftProjection existingShift : context.nearbyShifts()) {
            ShiftInterval existingInterval = context.toShiftInterval(
                    existingShift.getDate(),
                    existingShift.getStartTime(),
                    existingShift.getEndTime()
            );
            if (existingInterval != null && context.overlaps(proposedInterval, existingInterval)) {
                return List.of(new ConflictItem(FIELD, MESSAGE));
            }
        }

        return List.of();
    }
}
