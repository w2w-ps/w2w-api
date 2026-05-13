package com.w2w.api.scheduling;

import com.w2w.api.employee.model.Employee;
import com.w2w.api.scheduling.dto.ConflictItem;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@Order(10)
class MaxDailyHoursRule implements SchedulingValidationRule {
    private static final String FIELD = "MAX_DAILY_HOURS";
    private static final String MESSAGE_TEMPLATE = "%s is over their max hours per day on %s";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasShift() || context.employeeId() == null || context.shiftDate() == null) {
            return List.of();
        }

        Float proposedDuration = context.proposedDuration();
        if (proposedDuration == null) {
            return List.of();
        }

        Employee employee = context.employee();
        if (employee == null || employee.getMaxDailyHours() == null) {
            return List.of();
        }

        Map<LocalDate, Float> existingHoursByDate = context.hoursByDate(context.nearbyShifts());
        for (Map.Entry<LocalDate, Float> proposedDay : context.proposedHoursByDate().entrySet()) {
            float totalHours = existingHoursByDate.getOrDefault(proposedDay.getKey(), 0.0f) + proposedDay.getValue();
            if (totalHours >= employee.getMaxDailyHours()) {
                return List.of(new ConflictItem(
                        FIELD,
                        MESSAGE_TEMPLATE.formatted(context.employeeDisplayName(), context.weekdayName(proposedDay.getKey()))
                ));
            }
        }

        return List.of();
    }
}
