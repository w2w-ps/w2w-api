package com.w2w.api.scheduling;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.employee.EmployeeService;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.preferences.PreferencesService;
import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.timeoff.TimeOffRequest;
import com.w2w.api.timeoff.TimeOffService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

final class SchedulingValidationContext {
    private final UpdateShiftRequest shift;
    private final PreferencesService preferencesService;
    private final Supplier<Integer> companyId;
    private final Supplier<Float> proposedDuration;
    private final Supplier<ShiftInterval> proposedInterval;
    private final Supplier<List<PreferenceWindow>> preferenceWindows;
    private final Supplier<List<ShiftDateWindow>> coveredWindows;
    private final Supplier<Employee> employee;
    private final Supplier<List<DailyHoursShiftProjection>> nearbyShifts;
    private final Supplier<List<DailyHoursShiftProjection>> weeklyShifts;
    private final Supplier<List<Shift>> sameDayShifts;
    private final Supplier<List<TimeOffRequest>> blockingTimeOff;
    private final Map<LocalDate, Optional<String>> resolvedPreferences = new HashMap<>();

    SchedulingValidationContext(
            FindConflictRequest request,
            EmployeeService employeeService,
            SchedulingShiftLookupService shiftLookupService,
            PreferencesService preferencesService,
            TimeOffService timeOffService
    ) {
        this.shift = request == null ? null : request.shift();
        this.preferencesService = preferencesService;
        this.companyId = memoize(CurrentTenant::requireCurrentTenant);
        this.proposedDuration = memoize(() -> resolveDuration(shift));
        this.proposedInterval = memoize(() -> toShiftInterval(shiftDate(), startTime(), endTime()));
        this.preferenceWindows = memoize(() -> hasTimedShift()
                ? splitPreferenceWindows(shiftDate(), startTime(), endTime())
                : List.of());
        this.coveredWindows = memoize(() -> hasTimedShift()
                ? splitShiftDateWindows(shiftDate(), startTime(), endTime())
                : List.of());
        this.employee = memoize(() -> {
            if (employeeId() == null) {
                return null;
            }
            return employeeService.findActiveEmployeeForCurrentTenant(employeeId())
                    .orElse(null);
        });
        this.nearbyShifts = memoize(() -> {
            if (employeeId() == null || shiftDate() == null) {
                return List.of();
            }
            return emptyIfNull(shiftLookupService.findActiveDailyHoursShifts(
                    companyId(),
                    employeeId(),
                    shiftDate().minusDays(1),
                    shiftDate().plusDays(1),
                    shiftId()
            ));
        });
        this.weeklyShifts = memoize(() -> {
            if (employeeId() == null || shiftDate() == null) {
                return List.of();
            }
            return emptyIfNull(shiftLookupService.findActiveDailyHoursShifts(
                    companyId(),
                    employeeId(),
                    startOfWeek(shiftDate()),
                    endOfWeek(shiftDate()),
                    shiftId()
            ));
        });
        this.sameDayShifts = memoize(() -> {
            if (employeeId() == null || shiftDate() == null) {
                return List.of();
            }
            return emptyIfNull(shiftLookupService.findActiveSameDayShifts(
                    companyId(),
                    employeeId(),
                    shiftDate(),
                    shiftId()
            ));
        });
        this.blockingTimeOff = memoize(() -> {
            if (employeeId() == null || coveredWindows().isEmpty()) {
                return List.of();
            }
            return emptyIfNull(timeOffService.findBlockingTimeOff(
                    employeeId(),
                    coveredStartDate(),
                    coveredEndDate()
            ));
        });
    }

    boolean hasShift() {
        return shift != null;
    }

    boolean hasTimedShift() {
        return hasShift()
                && employeeId() != null
                && shiftDate() != null
                && startTime() != null
                && endTime() != null;
    }

    Integer shiftId() {
        return shift == null ? null : shift.shiftId();
    }

    Integer employeeId() {
        return shift == null ? null : shift.employeeId();
    }

    LocalDate shiftDate() {
        return shift == null ? null : shift.date();
    }

    LocalTime startTime() {
        return shift == null ? null : shift.startTime();
    }

    LocalTime endTime() {
        return shift == null ? null : shift.endTime();
    }

    Float proposedDuration() {
        return proposedDuration.get();
    }

    ShiftInterval proposedInterval() {
        return proposedInterval.get();
    }

    List<PreferenceWindow> preferenceWindows() {
        return preferenceWindows.get();
    }

    List<ShiftDateWindow> coveredWindows() {
        return coveredWindows.get();
    }

    Employee employee() {
        return employee.get();
    }

    List<DailyHoursShiftProjection> nearbyShifts() {
        return nearbyShifts.get();
    }

    List<DailyHoursShiftProjection> weeklyShifts() {
        return weeklyShifts.get();
    }

    List<Shift> sameDayShifts() {
        return sameDayShifts.get();
    }

    List<TimeOffRequest> blockingTimeOff() {
        return blockingTimeOff.get();
    }

    String resolvedPreference(LocalDate date) {
        return resolvedPreferences.computeIfAbsent(date, key -> {
            Optional<String> preference = preferencesService.getResolvedPreference(employeeId(), key);
            return preference == null ? Optional.empty() : preference;
        }).orElse(null);
    }

    Map<LocalDate, Float> hoursByDate(List<DailyHoursShiftProjection> shifts) {
        Map<LocalDate, Float> hoursByDate = new LinkedHashMap<>();
        for (DailyHoursShiftProjection shift : shifts) {
            addHoursByDate(
                    hoursByDate,
                    shift.getDate(),
                    shift.getStartTime(),
                    shift.getEndTime(),
                    shift.getDuration()
            );
        }
        return hoursByDate;
    }

    Map<LocalDate, Float> proposedHoursByDate() {
        Map<LocalDate, Float> hoursByDate = new LinkedHashMap<>();
        addHoursByDate(hoursByDate, shiftDate(), startTime(), endTime(), proposedDuration());
        return hoursByDate;
    }

    float resolveDuration(DailyHoursShiftProjection shift) {
        if (shift.getDuration() != null) {
            return shift.getDuration();
        }
        if (shift.getStartTime() == null || shift.getEndTime() == null) {
            return 0.0f;
        }
        return calculateDurationHours(shift.getStartTime(), shift.getEndTime());
    }

    boolean overlaps(ShiftInterval first, ShiftInterval second) {
        return first.start().isBefore(second.end()) && second.start().isBefore(first.end());
    }

    ShiftInterval toShiftInterval(LocalDate date, LocalTime startTime, LocalTime endTime) {
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

    boolean overlapsTimeOff(TimeOffRequest timeOffRequest) {
        if (Boolean.TRUE.equals(timeOffRequest.getFullDay())) {
            return overlapsFullDayTimeOff(timeOffRequest);
        }
        return overlapsTimedTimeOff(timeOffRequest);
    }

    private Integer companyId() {
        return companyId.get();
    }

    private LocalDate coveredStartDate() {
        List<ShiftDateWindow> windows = coveredWindows();
        return windows.isEmpty() ? null : windows.get(0).date();
    }

    private LocalDate coveredEndDate() {
        List<ShiftDateWindow> windows = coveredWindows();
        return windows.isEmpty() ? null : windows.get(windows.size() - 1).date();
    }

    private Float resolveDuration(UpdateShiftRequest shift) {
        if (shift == null) {
            return null;
        }
        if (shift.duration() != null) {
            return shift.duration();
        }
        if (shift.startTime() == null || shift.endTime() == null) {
            return null;
        }
        return calculateDurationHours(shift.startTime(), shift.endTime());
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

    private List<PreferenceWindow> splitPreferenceWindows(LocalDate date, LocalTime startTime, LocalTime endTime) {
        int startMinute = startTime.getHour() * 60 + startTime.getMinute();
        int endMinute = endTime.getHour() * 60 + endTime.getMinute();

        if (endTime.isAfter(startTime)) {
            return List.of(new PreferenceWindow(date, startMinute, endMinute));
        }

        return List.of(
                new PreferenceWindow(date, startMinute, 24 * 60),
                new PreferenceWindow(date.plusDays(1), 0, endMinute)
        );
    }

    private List<ShiftDateWindow> splitShiftDateWindows(LocalDate date, LocalTime startTime, LocalTime endTime) {
        ShiftInterval shiftInterval = toShiftInterval(date, startTime, endTime);
        if (shiftInterval == null) {
            return List.of();
        }

        if (endTime.isAfter(startTime)) {
            return List.of(new ShiftDateWindow(date, shiftInterval));
        }

        LocalDate nextDate = date.plusDays(1);
        LocalDateTime midnight = LocalDateTime.of(nextDate, LocalTime.MIDNIGHT);
        return List.of(
                new ShiftDateWindow(date, new ShiftInterval(shiftInterval.start(), midnight)),
                new ShiftDateWindow(nextDate, new ShiftInterval(midnight, shiftInterval.end()))
        );
    }

    private boolean overlapsFullDayTimeOff(TimeOffRequest timeOffRequest) {
        if (timeOffRequest.getStartDate() == null || timeOffRequest.getEndDate() == null) {
            return false;
        }

        for (ShiftDateWindow coveredWindow : coveredWindows()) {
            if (!coveredWindow.date().isBefore(timeOffRequest.getStartDate())
                    && !coveredWindow.date().isAfter(timeOffRequest.getEndDate())) {
                return true;
            }
        }

        return false;
    }

    private boolean overlapsTimedTimeOff(TimeOffRequest timeOffRequest) {
        if (timeOffRequest.getStartDate() == null
                || timeOffRequest.getStartTime() == null
                || timeOffRequest.getEndTime() == null
                || !timeOffRequest.getEndTime().isAfter(timeOffRequest.getStartTime())) {
            return false;
        }

        ShiftInterval timeOffInterval = new ShiftInterval(
                LocalDateTime.of(timeOffRequest.getStartDate(), timeOffRequest.getStartTime()),
                LocalDateTime.of(timeOffRequest.getStartDate(), timeOffRequest.getEndTime())
        );

        for (ShiftDateWindow coveredWindow : coveredWindows()) {
            if (coveredWindow.date().equals(timeOffRequest.getStartDate())
                    && overlaps(coveredWindow.interval(), timeOffInterval)) {
                return true;
            }
        }

        return false;
    }

    private <T> Supplier<T> memoize(Supplier<T> supplier) {
        return new Supplier<>() {
            private boolean initialized;
            private T value;

            @Override
            public T get() {
                if (!initialized) {
                    value = supplier.get();
                    initialized = true;
                }
                return value;
            }
        };
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }
}

record ShiftInterval(LocalDateTime start, LocalDateTime end) {
}

record PreferenceWindow(LocalDate date, int startMinute, int endMinute) {
}

record ShiftDateWindow(LocalDate date, ShiftInterval interval) {
}
