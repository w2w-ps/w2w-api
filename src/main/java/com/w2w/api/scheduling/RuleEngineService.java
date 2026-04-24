package com.w2w.api.scheduling;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleEngineService {
    private static final String MAX_DAILY_HOURS_FIELD = "MAX_DAILY_HOURS";
    private static final String MAX_DAILY_HOURS_MESSAGE = "Total scheduled hours exceed the employee's maximum daily hours.";
    private static final String MAX_DAILY_SHIFTS_FIELD = "MAX_DAILY_SHIFTS";
    private static final String MAX_DAILY_SHIFTS_MESSAGE = "Total shifts exceed the employee's maximum daily shifts.";
    private static final String MAX_WEEKLY_HOURS_FIELD = "MAX_WEEKLY_HOURS";
    private static final String MAX_WEEKLY_HOURS_MESSAGE = "Total scheduled hours exceed the employee's maximum weekly hours.";
    private static final String MAX_WEEKLY_SHIFTS_FIELD = "MAX_WEEKLY_SHIFTS";
    private static final String MAX_WEEKLY_SHIFTS_MESSAGE = "Total shifts exceed the employee's maximum weekly shifts.";
    private static final String EXISTING_SHIFT_CONFLICT_FIELD = "shift";
    private static final String EXISTING_SHIFT_CONFLICT_MESSAGE = "Overlaps existing shift";

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;

    public RuleEngineService(EmployeeRepository employeeRepository, ShiftRepository shiftRepository) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
    }

    public List<ConflictItem> checkMaxHoursPerDay(FindConflictRequest request) {
        if (request == null || request.shift() == null) {
            return List.of();
        }

        UpdateShiftRequest shift = request.shift();
        if (shift.employeeId() == null || shift.date() == null) {
            return List.of();
        }

        Float proposedDuration = resolveDuration(shift);
        if (proposedDuration == null) {
            return List.of();
        }

        Integer companyId = CurrentTenant.requireCurrentTenant();
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyIdAndIsDeletedFalse(shift.employeeId(), companyId)
                .orElse(null);
        if (employee == null || employee.getMaxDailyHours() == null) {
            return List.of();
        }

        Map<LocalDate, Float> hoursByDate = new LinkedHashMap<>();
        shiftRepository.findActiveDailyHoursShiftsByCompanyIdAndEmployeeIdAndDateRangeExcludingShiftId(
                        companyId,
                        shift.employeeId(),
                        shift.date().minusDays(1),
                        shift.date().plusDays(1),
                        shift.shiftId()
                )
                .forEach(existingShift -> addHoursByDate(
                        hoursByDate,
                        existingShift.getDate(),
                        existingShift.getStartTime(),
                        existingShift.getEndTime(),
                        existingShift.getDuration()
                ));

        Map<LocalDate, Float> proposedHoursByDate = new LinkedHashMap<>();
        addHoursByDate(
                proposedHoursByDate,
                shift.date(),
                shift.startTime(),
                shift.endTime(),
                proposedDuration
        );

        for (Map.Entry<LocalDate, Float> proposedDay : proposedHoursByDate.entrySet()) {
            float totalHours = hoursByDate.getOrDefault(proposedDay.getKey(), 0.0f) + proposedDay.getValue();
            if (totalHours >= employee.getMaxDailyHours()) {
                return List.of(new ConflictItem(MAX_DAILY_HOURS_FIELD, MAX_DAILY_HOURS_MESSAGE));
            }
        }

        return List.of();
    }

    public List<ConflictItem> checkMaxShiftsPerDay(FindConflictRequest request) {
        if (request == null || request.shift() == null) {
            return List.of();
        }

        UpdateShiftRequest shift = request.shift();
        if (shift.employeeId() == null || shift.date() == null) {
            return List.of();
        }

        Integer companyId = CurrentTenant.requireCurrentTenant();
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyIdAndIsDeletedFalse(shift.employeeId(), companyId)
                .orElse(null);
        if (employee == null || employee.getMaxDailyShifts() == null) {
            return List.of();
        }

        int existingShiftCount = shiftRepository.findActiveByCompanyIdAndEmployeeIdAndDateExcludingShiftId(
                companyId,
                shift.employeeId(),
                shift.date(),
                shift.shiftId()
        ).size();

        int totalShiftCount = existingShiftCount + 1;
        if (totalShiftCount > employee.getMaxDailyShifts()) {
            return List.of(new ConflictItem(MAX_DAILY_SHIFTS_FIELD, MAX_DAILY_SHIFTS_MESSAGE));
        }

        return List.of();
    }

    public List<ConflictItem> checkMaxHoursAndShiftsPerWeek(FindConflictRequest request) {
        if (request == null || request.shift() == null) {
            return List.of();
        }

        UpdateShiftRequest shift = request.shift();
        if (shift.employeeId() == null || shift.date() == null) {
            return List.of();
        }

        Integer companyId = CurrentTenant.requireCurrentTenant();
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyIdAndIsDeletedFalse(shift.employeeId(), companyId)
                .orElse(null);
        if (employee == null) {
            return List.of();
        }

        LocalDate weekStart = startOfWeek(shift.date());
        LocalDate weekEnd = endOfWeek(shift.date());
        List<DailyHoursShiftProjection> weeklyShifts = shiftRepository
                .findActiveDailyHoursShiftsByCompanyIdAndEmployeeIdAndDateRangeExcludingShiftId(
                        companyId,
                        shift.employeeId(),
                        weekStart,
                        weekEnd,
                        shift.shiftId()
                );

        List<ConflictItem> conflicts = new ArrayList<>(2);

        if (employee.getMaxScheduledHours() != null) {
            Float proposedDuration = resolveDuration(shift);
            if (proposedDuration != null) {
                float existingWeeklyHours = 0.0f;
                for (DailyHoursShiftProjection existingShift : weeklyShifts) {
                    existingWeeklyHours += resolveDuration(existingShift);
                }

                float totalWeeklyHours = existingWeeklyHours + proposedDuration;
                if (totalWeeklyHours > employee.getMaxScheduledHours()) {
                    conflicts.add(new ConflictItem(MAX_WEEKLY_HOURS_FIELD, MAX_WEEKLY_HOURS_MESSAGE));
                }
            }
        }

        if (employee.getMaxWeeklyDays() != null) {
            int totalWeeklyShifts = weeklyShifts.size() + 1;
            if (totalWeeklyShifts > employee.getMaxWeeklyDays()) {
                conflicts.add(new ConflictItem(MAX_WEEKLY_SHIFTS_FIELD, MAX_WEEKLY_SHIFTS_MESSAGE));
            }
        }

        return conflicts;
    }

    public List<ConflictItem> checkWorkPreferences(FindConflictRequest request) {
        // TODO: Implement check for work preferences.
        return List.of();
    }

    public List<ConflictItem> checkTimeOff(FindConflictRequest request) {
        // TODO: Implement check for time off conflicts.
        return List.of();
    }

    public List<ConflictItem> checkConflictWithExistingShifts(FindConflictRequest request) {
        if (request == null || request.shift() == null) {
            return List.of();
        }

        UpdateShiftRequest shift = request.shift();
        if (shift.employeeId() == null || shift.date() == null || shift.startTime() == null || shift.endTime() == null) {
            return List.of();
        }

        ShiftInterval proposedInterval = toShiftInterval(shift.date(), shift.startTime(), shift.endTime());
        if (proposedInterval == null) {
            return List.of();
        }

        Integer companyId = CurrentTenant.requireCurrentTenant();
        List<DailyHoursShiftProjection> existingShifts = shiftRepository
                .findActiveDailyHoursShiftsByCompanyIdAndEmployeeIdAndDateRangeExcludingShiftId(
                        companyId,
                        shift.employeeId(),
                        shift.date().minusDays(1),
                        shift.date().plusDays(1),
                        shift.shiftId()
                );

        for (DailyHoursShiftProjection existingShift : existingShifts) {
            ShiftInterval existingInterval = toShiftInterval(
                    existingShift.getDate(),
                    existingShift.getStartTime(),
                    existingShift.getEndTime()
            );
            if (existingInterval != null && overlaps(proposedInterval, existingInterval)) {
                return List.of(new ConflictItem(EXISTING_SHIFT_CONFLICT_FIELD, EXISTING_SHIFT_CONFLICT_MESSAGE));
            }
        }

        return List.of();
    }

    private float resolveDuration(Shift shift) {
        if (shift.getDuration() != null) {
            return shift.getDuration();
        }
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            return 0.0f;
        }
        return calculateDurationHours(shift.getStartTime(), shift.getEndTime());
    }

    private Float resolveDuration(UpdateShiftRequest shift) {
        if (shift.duration() != null) {
            return shift.duration();
        }
        if (shift.startTime() == null || shift.endTime() == null) {
            return null;
        }
        return calculateDurationHours(shift.startTime(), shift.endTime());
    }

    private float resolveDuration(DailyHoursShiftProjection shift) {
        if (shift.getDuration() != null) {
            return shift.getDuration();
        }
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            return 0.0f;
        }
        return calculateDurationHours(shift.getStartTime(), shift.getEndTime());
    }

    private void addHoursByDate(
            Map<LocalDate, Float> hoursByDate,
            LocalDate shiftDate,
            LocalTime startTime,
            LocalTime endTime,
            Float duration
    ) {
        if (shiftDate == null) {
            return;
        }

        if (startTime == null || endTime == null) {
            if (duration != null) {
                addHours(hoursByDate, shiftDate, duration);
            }
            return;
        }

        long totalMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (totalMinutes < 0) {
            totalMinutes += 24 * 60;
        }

        float totalHours = duration != null ? duration : totalMinutes / 60.0f;
        if (totalMinutes <= 0) {
            addHours(hoursByDate, shiftDate, totalHours);
            return;
        }

        if (!endTime.isBefore(startTime)) {
            addHours(hoursByDate, shiftDate, totalHours);
            return;
        }

        long startDayMinutes = (24 * 60L) - startTime.toSecondOfDay() / 60L;
        long nextDayMinutes = totalMinutes - startDayMinutes;
        float hoursPerMinute = totalHours / totalMinutes;

        addHours(hoursByDate, shiftDate, startDayMinutes * hoursPerMinute);
        addHours(hoursByDate, shiftDate.plusDays(1), nextDayMinutes * hoursPerMinute);
    }

    private void addHours(Map<LocalDate, Float> hoursByDate, LocalDate date, float hours) {
        hoursByDate.merge(date, hours, Float::sum);
    }

    private ShiftInterval toShiftInterval(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null || startTime == null || endTime == null) {
            return null;
        }

        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end = LocalDateTime.of(date, endTime);
        if (!end.isAfter(start)) {
            end = end.plusDays(1);
        }

        return new ShiftInterval(start, end);
    }

    private boolean overlaps(ShiftInterval first, ShiftInterval second) {
        return first.start().isBefore(second.end()) && second.start().isBefore(first.end());
    }

    private LocalDate startOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate endOfWeek(LocalDate date) {
        return startOfWeek(date).plusDays(6);
    }

    private float calculateDurationHours(LocalTime startTime, LocalTime endTime) {
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return minutes / 60.0f;
    }

    private record ShiftInterval(LocalDateTime start, LocalDateTime end) {
    }
}
