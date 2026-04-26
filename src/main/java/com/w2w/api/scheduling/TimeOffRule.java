package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.timeoff.TimeOffRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(50)
class TimeOffRule implements SchedulingValidationRule {
    private static final String FIELD = "TIME_OFF";
    private static final String MESSAGE = "Overlaps employee time off";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasTimedShift() || context.coveredWindows().isEmpty()) {
            return List.of();
        }

        for (TimeOffRequest timeOffRequest : context.blockingTimeOff()) {
            if (context.overlapsTimeOff(timeOffRequest)) {
                return List.of(new ConflictItem(FIELD, MESSAGE));
            }
        }

        return List.of();
    }
}
