package com.w2w.api.scheduling;

import com.w2w.api.employee.model.Employee;
import com.w2w.api.scheduling.dto.ConflictItem;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(20)
class MaxDailyShiftsRule implements SchedulingValidationRule {
    private static final String FIELD = "MAX_DAILY_SHIFTS";
    private static final String MESSAGE = "Total shifts exceed the employee's maximum daily shifts.";

    @Override
    public List<ConflictItem> validate(SchedulingValidationContext context) {
        if (!context.hasShift() || context.employeeId() == null || context.shiftDate() == null) {
            return List.of();
        }

        Employee employee = context.employee();
        if (employee == null || employee.getMaxDailyShifts() == null) {
            return List.of();
        }

        int totalShiftCount = context.sameDayShifts().size() + 1;
        if (totalShiftCount > employee.getMaxDailyShifts()) {
            return List.of(new ConflictItem(FIELD, MESSAGE));
        }

        return List.of();
    }
}
