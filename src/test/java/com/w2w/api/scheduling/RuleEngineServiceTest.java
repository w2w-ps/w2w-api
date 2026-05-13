package com.w2w.api.scheduling;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.EmployeeService;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.preferences.PreferencesService;
import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.timeoff.TimeOffRequest;
import com.w2w.api.timeoff.TimeOffService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RuleEngineServiceTest {
    private EmployeeService employeeService;
    private SchedulingShiftLookupService shiftLookupService;
    private PreferencesService preferencesService;
    private TimeOffService timeOffService;
    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);
        shiftLookupService = mock(SchedulingShiftLookupService.class);
        preferencesService = mock(PreferencesService.class);
        timeOffService = mock(TimeOffService.class);
        ruleEngineService = new RuleEngineService(
                employeeService,
                shiftLookupService,
                preferencesService,
                timeOffService,
                List.of(
                        new MaxDailyHoursRule(),
                        new MaxDailyShiftsRule(),
                        new MaxWeeklyHoursAndShiftsRule(),
                        new WorkPreferencesRule(),
                        new TimeOffRule(),
                        new ExistingShiftConflictRule()
                )
        );
        TenantContext.setCurrentTenant(1);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void checkMaxHoursPerDay_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void validate_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.validate(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService, preferencesService, timeOffService);
    }

    @Test
    void validate_accumulatesConflictsInRuleOrder() {
        Employee employee = employeeWithMaxDailyHours(8);
        employee.setMaxDailyShifts(1);
        employee.setMaxScheduledHours(8);
        employee.setMaxWeeklyDays(1);

        DailyHoursShiftProjection overlappingShift = existingShift(
                LocalDate.of(2026, 4, 21),
                5.0f,
                LocalTime.of(12, 0),
                LocalTime.of(20, 0)
        );

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of(overlappingShift));
        when(shiftLookupService.findActiveSameDayShifts(
                1,
                101,
                LocalDate.of(2026, 4, 21),
                null
        )).thenReturn(List.of(existingScheduledShift()));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(overlappingShift));
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 40, 'D')));
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)))
                .thenReturn(List.of(timedTimeOff(
                        LocalDate.of(2026, 4, 21),
                        LocalTime.of(12, 0),
                        LocalTime.of(13, 0),
                        1,
                        "APPROVED"
                )));

        List<ConflictItem> conflicts = ruleEngineService.validate(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                4.0f
        ));

        assertEquals(List.of(
                new ConflictItem("MAX_DAILY_HOURS", "Richard Comp is over their max hours per day on Tuesday"),
                new ConflictItem("MAX_DAILY_SHIFTS", "Richard Comp is over their max shifts per day on Tuesday"),
                new ConflictItem("MAX_WEEKLY_HOURS", "Richard Comp is over their max hours per week."),
                new ConflictItem("MAX_WEEKLY_SHIFTS", "Richard Comp is over their max shifts per week."),
                new ConflictItem("WORK_PREFERENCES", "Richard Comp is set to DISLIKES WORK at this time on Tuesday."),
                new ConflictItem("TIME_OFF", "Richard Comp is OFF at this time on Tuesday"),
                new ConflictItem("shift", "Richard Comp is already assigned to a shift at the same time on Tuesday.")
        ), conflicts);
    }

    @Test
    void checkMaxHoursPerDay_employeeWithoutMaxDailyHours_returnsNoConflicts() {
        Employee employee = new Employee();
        employee.setEmployeeId(101);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxHoursPerDay_totalBelowLimit_returnsNoConflicts() {
        Employee employee = employeeWithMaxDailyHours(8);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 21), 2.5f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        ))
                .thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                5.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxHoursPerDay_totalAtLimit_returnsConflict() {
        Employee employee = employeeWithMaxDailyHours(8);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 21), 3.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        ))
                .thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                5.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_DAILY_HOURS",
                "Richard Comp is over their max hours per day on Tuesday"
        )), conflicts);
    }

    @Test
    void checkMaxHoursPerDay_updateExcludesCurrentShiftFromExistingHours() {
        Employee employee = employeeWithMaxDailyHours(9);
        DailyHoursShiftProjection otherShift = existingShift(LocalDate.of(2026, 4, 21), 2.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                9001
        ))
                .thenReturn(List.of(otherShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                9001,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                6.0f
        ));

        assertTrue(conflicts.isEmpty());
        verify(shiftLookupService).findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                9001
        );
    }

    @Test
    void checkMaxHoursPerDay_previousDayOvernightSpilloverConflictsOnRequestedDate() {
        Employee employee = employeeWithMaxDailyHours(6);
        DailyHoursShiftProjection previousDayOvernight = existingShift(
                LocalDate.of(2026, 4, 20),
                null,
                LocalTime.of(22, 0),
                LocalTime.of(2, 0)
        );

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        ))
                .thenReturn(List.of(previousDayOvernight));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(14, 0),
                4.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_DAILY_HOURS",
                "Richard Comp is over their max hours per day on Tuesday"
        )), conflicts);
    }

    @Test
    void checkMaxHoursPerDay_proposedOvernightConflictsOnNextDay() {
        Employee employee = employeeWithMaxDailyHours(5);
        DailyHoursShiftProjection nextDayShift = existingShift(
                LocalDate.of(2026, 4, 22),
                3.0f,
                LocalTime.of(8, 0),
                LocalTime.of(11, 0)
        );

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        ))
                .thenReturn(List.of(nextDayShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(22, 0),
                LocalTime.of(2, 0),
                4.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_DAILY_HOURS",
                "Richard Comp is over their max hours per day on Wednesday"
        )), conflicts);
    }

    @Test
    void checkMaxHoursPerDay_missingDurationAndTimes_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                null
        ));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkMaxShiftsPerDay_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkMaxShiftsPerDay_employeeWithoutMaxDailyShifts_returnsNoConflicts() {
        Employee employee = new Employee();
        employee.setEmployeeId(101);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxShiftsPerDay_noExistingShiftWithMaxOne_returnsNoConflicts() {
        Employee employee = employeeWithMaxDailyShifts(1);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveSameDayShifts(1, 101, LocalDate.of(2026, 4, 21), null))
                .thenReturn(List.of());

        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxShiftsPerDay_existingOneWithMaxOne_returnsConflict() {
        Employee employee = employeeWithMaxDailyShifts(1);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveSameDayShifts(1, 101, LocalDate.of(2026, 4, 21), null))
                .thenReturn(List.of(existingScheduledShift()));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_DAILY_SHIFTS",
                "Richard Comp is over their max shifts per day on Tuesday"
        )), conflicts);
    }

    @Test
    void checkMaxShiftsPerDay_existingOneWithMaxTwo_returnsNoConflicts() {
        Employee employee = employeeWithMaxDailyShifts(2);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveSameDayShifts(1, 101, LocalDate.of(2026, 4, 21), null))
                .thenReturn(List.of(existingScheduledShift()));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxShiftsPerDay_updateExcludesCurrentShiftAndRemainsAllowed() {
        Employee employee = employeeWithMaxDailyShifts(1);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveSameDayShifts(1, 101, LocalDate.of(2026, 4, 21), 9001))
                .thenReturn(List.of());

        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                9001,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
        verify(shiftLookupService).findActiveSameDayShifts(1, 101, LocalDate.of(2026, 4, 21), 9001);
    }

    @Test
    void checkMaxShiftsPerDay_missingEmployeeIdOrDate_returnsNoConflicts() {
        List<ConflictItem> missingEmployeeConflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                null,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingDateConflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                101,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(missingEmployeeConflicts.isEmpty());
        assertTrue(missingDateConflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkMaxShiftsPerDay_overnightShiftCountsAsOneShiftForStartDate() {
        Employee employee = employeeWithMaxDailyShifts(1);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveSameDayShifts(1, 101, LocalDate.of(2026, 4, 21), null))
                .thenReturn(List.of(existingOvernightShift()));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxShiftsPerDay(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_DAILY_SHIFTS",
                "Richard Comp is over their max shifts per day on Tuesday"
        )), conflicts);
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_missingEmployeeIdOrDate_returnsNoConflicts() {
        List<ConflictItem> missingEmployeeConflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                null,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingDateConflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(missingEmployeeConflicts.isEmpty());
        assertTrue(missingDateConflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_employeeWithoutWeeklyLimits_returnsNoConflicts() {
        Employee employee = new Employee();
        employee.setEmployeeId(101);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 21), 8.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_weeklyHoursBelowLimit_returnsNoConflicts() {
        Employee employee = employeeWithWeeklyLimits(10, null);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 22), 4.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(14, 0),
                5.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_weeklyHoursAboveLimit_returnsConflict() {
        Employee employee = employeeWithWeeklyLimits(8, null);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 22), 5.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                4.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_WEEKLY_HOURS",
                "Richard Comp is over their max hours per week."
        )), conflicts);
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_weeklyShiftsAtLimit_returnsNoConflicts() {
        Employee employee = employeeWithWeeklyLimits(null, 2);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 22), 5.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                null
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_weeklyShiftsAboveLimit_returnsConflict() {
        Employee employee = employeeWithWeeklyLimits(null, 1);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 22), 5.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                null,
                null
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_WEEKLY_SHIFTS",
                "Richard Comp is over their max shifts per week."
        )), conflicts);
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_updateExcludesCurrentShiftAndUsesWeekBounds() {
        Employee employee = employeeWithWeeklyLimits(8, 1);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                9001
        )).thenReturn(List.of());

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                9001,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
        verify(shiftLookupService).findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                9001
        );
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_currentWeekOvernightCountsFullDuration() {
        Employee employee = employeeWithWeeklyLimits(7, null);
        DailyHoursShiftProjection overnightShift = existingShift(
                LocalDate.of(2026, 4, 26),
                null,
                LocalTime.of(22, 0),
                LocalTime.of(2, 0)
        );

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(overnightShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                4.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "MAX_WEEKLY_HOURS",
                "Richard Comp is over their max hours per week."
        )), conflicts);
    }

    @Test
    void checkMaxHoursAndShiftsPerWeek_bothWeeklyLimitsExceeded_returnsBothConflicts() {
        Employee employee = employeeWithWeeklyLimits(8, 1);
        DailyHoursShiftProjection existingShift = existingShift(LocalDate.of(2026, 4, 22), 5.0f, null, null);

        when(employeeService.findActiveEmployeeForCurrentTenant(101))
                .thenReturn(Optional.of(employee));
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 26),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkMaxHoursAndShiftsPerWeek(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                4.0f
        ));

        assertEquals(List.of(
                new ConflictItem(
                        "MAX_WEEKLY_HOURS",
                        "Richard Comp is over their max hours per week."
                ),
                new ConflictItem(
                        "MAX_WEEKLY_SHIFTS",
                        "Richard Comp is over their max shifts per week."
                )
        ), conflicts);
    }

    @Test
    void checkWorkPreferences_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService, preferencesService);
    }

    @Test
    void checkWorkPreferences_noResolvedPreference_returnsNoConflicts() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.empty());

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkWorkPreferences_allPreferredOrNeutral_returnsNoConflicts() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of("P".repeat(48) + "N".repeat(48)));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkWorkPreferences_anyDislikesSlot_returnsConflict() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 40, 'D')));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                0.5f
        ));

        assertEquals(List.of(new ConflictItem(
                "WORK_PREFERENCES",
                "Employee 101 is set to DISLIKES WORK at this time on Tuesday."
        )), conflicts);
    }

    @Test
    void checkWorkPreferences_anyCannotWorkSlot_returnsConflict() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 40, 'C')));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                0.5f
        ));

        assertEquals(List.of(new ConflictItem(
                "WORK_PREFERENCES",
                "Employee 101 is set to CANNOT WORK at this time on Tuesday."
        )), conflicts);
    }

    @Test
    void checkWorkPreferences_cannotWorkTakesPriorityOverDislikes() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 40, 'D', 41, 'C')));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                0.5f
        ));

        assertEquals(List.of(new ConflictItem(
                "WORK_PREFERENCES",
                "Employee 101 is set to CANNOT WORK at this time on Tuesday."
        )), conflicts);
    }

    @Test
    void checkWorkPreferences_checksEveryIntersectingPartialSlot() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 36, 'D', 40, 'D')));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 1),
                LocalTime.of(10, 1),
                1.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "WORK_PREFERENCES",
                "Employee 101 is set to DISLIKES WORK at this time on Tuesday."
        )), conflicts);
    }

    @Test
    void checkWorkPreferences_overnightShiftChecksBothDatesWithinCoveredWindows() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 93, 'C')));
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 22)))
                .thenReturn(Optional.of(preferencesWith('P', 2, 'D', 6, 'C')));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(23, 30),
                LocalTime.of(1, 30),
                2.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "WORK_PREFERENCES",
                "Employee 101 is set to DISLIKES WORK at this time on Wednesday."
        )), conflicts);
        verify(preferencesService).getResolvedPreference(101, LocalDate.of(2026, 4, 21));
        verify(preferencesService).getResolvedPreference(101, LocalDate.of(2026, 4, 22));
    }

    @Test
    void checkWorkPreferences_usesResolvedPreferenceWithoutDuplicatingMergeLogic() {
        when(preferencesService.getResolvedPreference(101, LocalDate.of(2026, 4, 21)))
                .thenReturn(Optional.of(preferencesWith('P', 40, 'D')));

        List<ConflictItem> conflicts = ruleEngineService.checkWorkPreferences(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(10, 15),
                0.25f
        ));

        assertEquals(List.of(new ConflictItem(
                "WORK_PREFERENCES",
                "Employee 101 is set to DISLIKES WORK at this time on Tuesday."
        )), conflicts);
        verify(preferencesService).getResolvedPreference(101, LocalDate.of(2026, 4, 21));
    }

    @Test
    void checkTimeOff_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService, preferencesService, timeOffService);
    }

    @Test
    void checkTimeOff_missingRequiredFields_returnsNoConflicts() {
        List<ConflictItem> missingEmployeeConflicts = ruleEngineService.checkTimeOff(request(
                null,
                null,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingDateConflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingStartConflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingEndConflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                null,
                8.0f
        ));

        assertTrue(missingEmployeeConflicts.isEmpty());
        assertTrue(missingDateConflicts.isEmpty());
        assertTrue(missingStartConflicts.isEmpty());
        assertTrue(missingEndConflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService, preferencesService, timeOffService);
    }

    @Test
    void checkTimeOff_noQualifyingTimeOff_returnsNoConflicts() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)))
                .thenReturn(List.of());

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
        verify(timeOffService).findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21));
    }

    @Test
    void checkTimeOff_fullDayTimeOffCoveringShiftDate_returnsConflict() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)))
                .thenReturn(List.of(fullDayTimeOff(
                        LocalDate.of(2026, 4, 21),
                        LocalDate.of(2026, 4, 21),
                        "APPROVED"
                )));

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem("TIME_OFF", "Employee 101 is OFF at this time on Tuesday")), conflicts);
    }

    @Test
    void checkTimeOff_multiDayFullDayTimeOffCoveringOvernightShift_returnsConflict() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 22)))
                .thenReturn(List.of(fullDayTimeOff(
                        LocalDate.of(2026, 4, 22),
                        LocalDate.of(2026, 4, 23),
                        "PENDING"
                )));

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem("TIME_OFF", "Employee 101 is OFF at this time on Wednesday")), conflicts);
        verify(timeOffService).findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 22));
    }

    @Test
    void checkTimeOff_sameDayTimedOverlap_returnsConflict() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)))
                .thenReturn(List.of(timedTimeOff(
                        LocalDate.of(2026, 4, 21),
                        LocalTime.of(12, 0),
                        LocalTime.of(14, 0),
                        3,
                        "APPROVED"
                )));

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem("TIME_OFF", "Employee 101 is OFF at this time on Tuesday")), conflicts);
    }

    @Test
    void checkTimeOff_boundaryTouchingTimedTimeOff_returnsNoConflicts() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)))
                .thenReturn(List.of(timedTimeOff(
                        LocalDate.of(2026, 4, 21),
                        LocalTime.of(17, 0),
                        LocalTime.of(18, 0),
                        1,
                        "APPROVED"
                )));

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkTimeOff_overnightShiftOverlappingTimedTimeOffOnNextDate_returnsConflict() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 22)))
                .thenReturn(List.of(timedTimeOff(
                        LocalDate.of(2026, 4, 22),
                        LocalTime.of(1, 0),
                        LocalTime.of(3, 0),
                        2,
                        "APPROVED"
                )));

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem("TIME_OFF", "Employee 101 is OFF at this time on Wednesday")), conflicts);
    }

    @Test
    void checkTimeOff_timedTimeOffOutsideCoveredDates_returnsNoConflicts() {
        when(timeOffService.findBlockingTimeOff(101, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)))
                .thenReturn(List.of(timedTimeOff(
                        LocalDate.of(2026, 4, 20),
                        LocalTime.of(12, 0),
                        LocalTime.of(14, 0),
                        4,
                        "APPROVED"
                )));

        List<ConflictItem> conflicts = ruleEngineService.checkTimeOff(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkConflictWithExistingShifts_withoutShift_returnsNoConflicts() {
        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(new FindConflictRequest(1, null, null));

        assertTrue(conflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkConflictWithExistingShifts_missingRequiredFields_returnsNoConflicts() {
        List<ConflictItem> missingEmployeeConflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                null,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingDateConflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingStartConflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                null,
                LocalTime.of(17, 0),
                8.0f
        ));
        List<ConflictItem> missingEndConflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                null,
                8.0f
        ));

        assertTrue(missingEmployeeConflicts.isEmpty());
        assertTrue(missingDateConflicts.isEmpty());
        assertTrue(missingStartConflicts.isEmpty());
        assertTrue(missingEndConflicts.isEmpty());
        verifyNoInteractions(employeeService, shiftLookupService);
    }

    @Test
    void checkConflictWithExistingShifts_noExistingShift_returnsNoConflicts() {
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of());

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkConflictWithExistingShifts_overlappingSameDayShift_returnsConflict() {
        DailyHoursShiftProjection existingShift = existingShift(
                LocalDate.of(2026, 4, 21),
                8.0f,
                LocalTime.of(12, 0),
                LocalTime.of(20, 0)
        );

        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                8.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "shift",
                "Employee 101 is already assigned to a shift at the same time on Tuesday."
        )), conflicts);
    }

    @Test
    void checkConflictWithExistingShifts_boundaryTouchingShift_returnsNoConflicts() {
        DailyHoursShiftProjection existingShift = existingShift(
                LocalDate.of(2026, 4, 21),
                8.0f,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        );

        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(17, 0),
                LocalTime.of(21, 0),
                4.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkConflictWithExistingShifts_updateExcludesCurrentShift_returnsNoConflicts() {
        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                9001
        )).thenReturn(List.of());

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                9001,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f
        ));

        assertTrue(conflicts.isEmpty());
        verify(shiftLookupService).findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                9001
        );
    }

    @Test
    void checkConflictWithExistingShifts_previousDayOvernightOverlap_returnsConflict() {
        DailyHoursShiftProjection existingShift = existingShift(
                LocalDate.of(2026, 4, 20),
                4.0f,
                LocalTime.of(22, 0),
                LocalTime.of(2, 0)
        );

        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(1, 0),
                LocalTime.of(5, 0),
                4.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "shift",
                "Employee 101 is already assigned to a shift at the same time on Tuesday."
        )), conflicts);
    }

    @Test
    void checkConflictWithExistingShifts_previousDayOvernightBoundaryTouch_returnsNoConflicts() {
        DailyHoursShiftProjection existingShift = existingShift(
                LocalDate.of(2026, 4, 20),
                4.0f,
                LocalTime.of(22, 0),
                LocalTime.of(2, 0)
        );

        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(2, 0),
                LocalTime.of(6, 0),
                4.0f
        ));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void checkConflictWithExistingShifts_nextDayExistingShiftOverlapsProposedOvernight_returnsConflict() {
        DailyHoursShiftProjection existingShift = existingShift(
                LocalDate.of(2026, 4, 22),
                4.0f,
                LocalTime.of(1, 0),
                LocalTime.of(5, 0)
        );

        when(shiftLookupService.findActiveDailyHoursShifts(
                1,
                101,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 22),
                null
        )).thenReturn(List.of(existingShift));

        List<ConflictItem> conflicts = ruleEngineService.checkConflictWithExistingShifts(request(
                null,
                101,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(22, 0),
                LocalTime.of(2, 0),
                4.0f
        ));

        assertEquals(List.of(new ConflictItem(
                "shift",
                "Employee 101 is already assigned to a shift at the same time on Wednesday."
        )), conflicts);
    }

    private Employee employeeWithMaxDailyHours(int maxDailyHours) {
        Employee employee = new Employee();
        employee.setEmployeeId(101);
        employee.setFirstName("Richard");
        employee.setLastName("Comp");
        employee.setMaxDailyHours(maxDailyHours);
        return employee;
    }

    private Employee employeeWithMaxDailyShifts(int maxDailyShifts) {
        Employee employee = new Employee();
        employee.setEmployeeId(101);
        employee.setFirstName("Richard");
        employee.setLastName("Comp");
        employee.setMaxDailyShifts(maxDailyShifts);
        return employee;
    }

    private Employee employeeWithWeeklyLimits(Integer maxScheduledHours, Integer maxWeeklyDays) {
        Employee employee = new Employee();
        employee.setEmployeeId(101);
        employee.setFirstName("Richard");
        employee.setLastName("Comp");
        employee.setMaxScheduledHours(maxScheduledHours);
        employee.setMaxWeeklyDays(maxWeeklyDays);
        return employee;
    }

    private DailyHoursShiftProjection existingShift(
            LocalDate date,
            Float duration,
            LocalTime startTime,
            LocalTime endTime
    ) {
        DailyHoursShiftProjection shift = mock(DailyHoursShiftProjection.class);
        when(shift.getDate()).thenReturn(date);
        when(shift.getDuration()).thenReturn(duration);
        when(shift.getStartTime()).thenReturn(startTime);
        when(shift.getEndTime()).thenReturn(endTime);
        return shift;
    }

    private Shift existingScheduledShift() {
        return new Shift();
    }

    private Shift existingOvernightShift() {
        Shift shift = new Shift();
        shift.setIsOvernight(true);
        return shift;
    }

    private TimeOffRequest fullDayTimeOff(LocalDate startDate, LocalDate endDate, String status) {
        TimeOffRequest request = new TimeOffRequest();
        request.setEmployeeId(101);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setFullDay(true);
        request.setStatus(status);
        return request;
    }

    private TimeOffRequest timedTimeOff(
            LocalDate startDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer repeatCount,
            String status
    ) {
        TimeOffRequest request = new TimeOffRequest();
        request.setEmployeeId(101);
        request.setStartDate(startDate);
        request.setEndDate(startDate);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setRepeatCount(repeatCount);
        request.setFullDay(false);
        request.setStatus(status);
        return request;
    }

    private String preferencesWith(char defaultPreference, int slotIndex, char preference) {
        return preferencesWith(defaultPreference, new int[] { slotIndex }, new char[] { preference });
    }

    private String preferencesWith(
            char defaultPreference,
            int firstSlotIndex,
            char firstPreference,
            int secondSlotIndex,
            char secondPreference
    ) {
        return preferencesWith(
                defaultPreference,
                new int[] { firstSlotIndex, secondSlotIndex },
                new char[] { firstPreference, secondPreference }
        );
    }

    private String preferencesWith(char defaultPreference, int[] slotIndexes, char[] preferences) {
        char[] values = String.valueOf(defaultPreference).repeat(96).toCharArray();
        for (int i = 0; i < slotIndexes.length; i++) {
            values[slotIndexes[i]] = preferences[i];
        }
        return new String(values);
    }

    private FindConflictRequest request(
            Integer shiftId,
            Integer employeeId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Float duration
    ) {
        return new FindConflictRequest(
                99,
                shiftId == null ? "CREATE" : "UPDATE",
                new UpdateShiftRequest(
                        shiftId,
                        employeeId,
                        "Shift",
                        startTime,
                        endTime,
                        12,
                        7,
                        (short) 1,
                        date,
                        duration
                )
        );
    }
}
