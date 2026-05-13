package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(60)
class ExistingShiftConflictRule implements SchedulingValidationRule {
    private static final String FIELD = "shift";
    private static final String MESSAGE_TEMPLATE = "%s is already assigned to a shift at the same time on %s.";

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
            LocalDate overlapDate = context.overlapDate(proposedInterval, existingInterval);
            if (overlapDate != null) {
                return List.of(new ConflictItem(
                        FIELD,
                        MESSAGE_TEMPLATE.formatted(context.employeeDisplayName(), context.weekdayName(overlapDate))
                ));
            }
        }

        return List.of();
    }
}
