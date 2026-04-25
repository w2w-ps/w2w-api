package com.w2w.api.scheduling;

import com.w2w.api.employee.model.Employee;
import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(30)
class MaxWeeklyHoursAndShiftsRule implements SchedulingValidationRule {
    private static final String MAX_WEEKLY_HOURS_FIELD = "MAX_WEEKLY_HOURS";
    private static final String MAX_WEEKLY_HOURS_MESSAGE = "Total scheduled hours exceed the employee's maximum weekly hours.";
    private static final String MAX_WEEKLY_SHIFTS_FIELD = "MAX_WEEKLY_SHIFTS";
    private static final String MAX_WEEKLY_SHIFTS_MESSAGE = "Total shifts exceed the employee's maximum weekly shifts.";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasShift() || context.employeeId() == null || context.shiftDate() == null) {
            return List.of();
        }

        Employee employee = context.employee();
        if (employee == null) {
            return List.of();
        }

        if (employee.getMaxScheduledHours() == null && employee.getMaxWeeklyDays() == null) {
            return List.of();
        }

        List<ConflictItem> conflicts = new ArrayList<>(2);

        if (employee.getMaxScheduledHours() != null) {
            Float proposedDuration = context.proposedDuration();
            if (proposedDuration != null) {
                float existingWeeklyHours = 0.0f;
                for (DailyHoursShiftProjection existingShift : context.weeklyShifts()) {
                    existingWeeklyHours += context.resolveDuration(existingShift);
                }

                float totalWeeklyHours = existingWeeklyHours + proposedDuration;
                if (totalWeeklyHours > employee.getMaxScheduledHours()) {
                    conflicts.add(new ConflictItem(MAX_WEEKLY_HOURS_FIELD, MAX_WEEKLY_HOURS_MESSAGE));
                }
            }
        }

        if (employee.getMaxWeeklyDays() != null) {
            int totalWeeklyShifts = context.weeklyShifts().size() + 1;
            if (totalWeeklyShifts > employee.getMaxWeeklyDays()) {
                conflicts.add(new ConflictItem(MAX_WEEKLY_SHIFTS_FIELD, MAX_WEEKLY_SHIFTS_MESSAGE));
            }
        }

        return conflicts;
    }
}
