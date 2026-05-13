package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.timeoff.TimeOffRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(50)
class TimeOffRule implements SchedulingValidationRule {
    private static final String FIELD = "TIME_OFF";
    private static final String MESSAGE_TEMPLATE = "%s is OFF at this time on %s";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasTimedShift() || context.coveredWindows().isEmpty()) {
            return List.of();
        }

        for (TimeOffRequest timeOffRequest : context.blockingTimeOff()) {
            LocalDate overlapDate = context.timeOffOverlapDate(timeOffRequest);
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
