package com.w2w.api.scheduling;

import com.w2w.api.employee.EmployeeService;
import com.w2w.api.preferences.PreferencesService;
import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.timeoff.TimeOffService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleEngineService {
    private final EmployeeService employeeService;
    private final SchedulingShiftLookupService shiftLookupService;
    private final PreferencesService preferencesService;
    private final TimeOffService timeOffService;
    private final List<SchedulingValidationRule> rules;

    public RuleEngineService(
            EmployeeService employeeService,
            SchedulingShiftLookupService shiftLookupService,
            PreferencesService preferencesService,
            TimeOffService timeOffService,
            List<SchedulingValidationRule> rules
    ) {
        this.employeeService = employeeService;
        this.shiftLookupService = shiftLookupService;
        this.preferencesService = preferencesService;
        this.timeOffService = timeOffService;
        this.rules = List.copyOf(rules);
    }

    public List<ConflictItem> validate(FindConflictRequest request) {
        SchedulingValidationContext context = newContext(request);
        List<ConflictItem> conflicts = new ArrayList<>();
        for (SchedulingValidationRule rule : rules) {
            conflicts.addAll(rule.validate(context));
        }
        return conflicts;
    }

    List<ConflictItem> checkMaxHoursPerDay(FindConflictRequest request) {
        return new MaxDailyHoursRule().validate(newContext(request));
    }

    List<ConflictItem> checkMaxShiftsPerDay(FindConflictRequest request) {
        return new MaxDailyShiftsRule().validate(newContext(request));
    }

    List<ConflictItem> checkMaxHoursAndShiftsPerWeek(FindConflictRequest request) {
        return new MaxWeeklyHoursAndShiftsRule().validate(newContext(request));
    }

    List<ConflictItem> checkWorkPreferences(FindConflictRequest request) {
        return new WorkPreferencesRule().validate(newContext(request));
    }

    List<ConflictItem> checkTimeOff(FindConflictRequest request) {
        return new TimeOffRule().validate(newContext(request));
    }

    List<ConflictItem> checkConflictWithExistingShifts(FindConflictRequest request) {
        return new ExistingShiftConflictRule().validate(newContext(request));
    }

    private SchedulingValidationContext newContext(FindConflictRequest request) {
        return new SchedulingValidationContext(
                request,
                employeeService,
                shiftLookupService,
                preferencesService,
                timeOffService
        );
    }

}
