package com.w2w.api.scheduling;

import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.scheduling.dto.ConflictDto;
import com.w2w.api.scheduling.dto.DayPositionBucketDto;
import com.w2w.api.scheduling.dto.EmployeeScheduledShiftDto;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.PositionShiftBucketDto;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.ShiftDto;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SchedulingService {
    @Autowired
    private SchedulingQueryRepository schedulingQueryRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private RuleEngineService ruleEngineService;

    @Autowired
    private PositionService positionService;

    public ShiftResponseDto getShift(Integer shiftId) {
        ShiftDetailsProjection shift = shiftRepository.findShiftDetailsByShiftId(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));

        ShiftResponseDto response = new ShiftResponseDto();
        response.setShiftId(shift.getShiftId());
        response.setEmployeeId(shift.getEmployeeId());
        response.setCompanyId(shift.getCompanyId());
        response.setDescription(shift.getDescription());
        response.setStartTime(shift.getStartTime());
        response.setEndTime(shift.getEndTime());
        response.setDuration(shift.getDuration());
        response.setIsOvernight(shift.getIsOvernight());
        response.setDate(shift.getDate());
        response.setPosition(shift.getPosition());
        response.setCategory(shift.getCategory());
        response.setColor(shift.getColor());

        return response;
    }

    public Shift saveShift(CreateShiftRequest request) {
        Shift shift = new Shift();
        shift.setEmployeeId(request.getEmployeeId());
        shift.setCompanyId(request.getCompanyId());
        shift.setDescription(request.getDescription());
        shift.setStartTime(request.getStartTime());
        shift.setEndTime(request.getEndTime());
        shift.setRequiredSkillId(request.getPosition());
        shift.setCategoryId(request.getCategory());
        shift.setColor(request.getColor());
        shift.setIsDeleted(false);

        if (request.getDate() != null) {
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.getDate());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.getDuration());
        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public Shift updateShift(Integer shiftId, UpdateShiftRequest request) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));

        if (request.employeeId() != null) {
            shift.setEmployeeId(request.employeeId());
        }
        if (request.description() != null) {
            shift.setDescription(request.description());
        }
        if (request.startTime() != null) {
            shift.setStartTime(request.startTime());
        }
        if (request.endTime() != null) {
            shift.setEndTime(request.endTime());
        }
        if (request.position() != null) {
            shift.setRequiredSkillId(request.position());
        }
        if (request.category() != null) {
            shift.setCategoryId(request.category());
        }
        if (request.color() != null) {
            shift.setColor(request.color());
        }

        if (request.date() != null) {
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.date());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.duration());
        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public void softDeleteShift(Integer shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));
        shift.setIsDeleted(true);
        shiftRepository.save(shift);
    }

    public List<EmployeeWithShiftsDto> getEmployeeShiftsGroupedInRange(Integer companyId, LocalDate startDate, LocalDate endDate) {
        List<EmployeeShiftProjection> flatResults = findEmployeeShiftRows(companyId, startDate, endDate);
        Map<Integer, EmployeeWithShiftsDto> grouped = new LinkedHashMap<>();

        for (EmployeeShiftProjection row : flatResults) {
            EmployeeWithShiftsDto employeeDto = getOrCreateEmployeeDto(grouped, row, startDate, endDate);

            List<ShiftSegment> segments = buildShiftSegments(row, startDate, endDate);
            for (ShiftSegment segment : segments) {
                addShiftSegment(employeeDto, startDate, segment);
            }

            if (!segments.isEmpty()) {
                employeeDto.setShiftCount(employeeDto.getShiftCount() + 1);
            }
        }

        return new ArrayList<>(grouped.values());
    }

    public List<DayPositionBucketDto> getShiftsGroupedByDayAndPosition(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DayPositionBucketDto> grouped = initializeDayBuckets(
                startDate,
                endDate,
                getCompanyPositionNames(companyId)
        );
        List<ShiftSegment> segments = new ArrayList<>();

        for (EmployeeShiftProjection row : findEmployeeShiftRows(companyId, startDate, endDate)) {
            segments.addAll(buildShiftSegments(row, startDate, endDate));
        }

        segments.sort(Comparator
                .comparing(ShiftSegment::date)
                .thenComparing(segment -> segment.position() == null ? "" : segment.position())
                .thenComparing(ShiftSegment::startTime)
                .thenComparing(ShiftSegment::employeeId));

        for (ShiftSegment segment : segments) {
            DayPositionBucketDto dayBucket = grouped.get(segment.date());
            PositionShiftBucketDto positionBucket = getOrCreatePositionBucket(dayBucket, segment.position());
            positionBucket.shifts().add(new EmployeeScheduledShiftDto(
                    segment.shiftId(),
                    segment.employeeId(),
                    segment.firstName(),
                    segment.lastName(),
                    segment.phones(),
                    segment.startTime(),
                    segment.endTime(),
                    segment.category(),
                    segment.description(),
                    segment.durationHours(),
                    segment.color()
            ));
            updatePositionBucket(dayBucket, positionBucket, segment.durationHours());
            grouped.put(
                    segment.date(),
                    new DayPositionBucketDto(
                            dayBucket.date(),
                            dayBucket.positions(),
                            dayBucket.shiftCount() + 1,
                            dayBucket.totalDuration() + segment.durationHours()
                    )
            );
        }

        return new ArrayList<>(grouped.values());
    }

    public List<ConflictDto> validate(FindConflictRequest request) {
        List<ConflictDto> conflicts = new ArrayList<>();
        conflicts.addAll(ruleEngineService.checkMaxHoursPerDay(request));
        conflicts.addAll(ruleEngineService.checkMaxShiftsPerDay(request));
        conflicts.addAll(ruleEngineService.checkMaxHoursAndShiftsPerWeek(request));
        conflicts.addAll(ruleEngineService.checkWorkPreferences(request));
        conflicts.addAll(ruleEngineService.checkTimeOff(request));
        conflicts.addAll(ruleEngineService.checkConflictWithExistingShifts(request));
        return conflicts;
    }

    private Schedule getOrCreateSchedule(Integer companyId, LocalDate date) {
        return scheduleRepository.findByCompanyIdAndStartDate(companyId, date)
                .orElseGet(() -> {
                    Schedule newSchedule = new Schedule();
                    newSchedule.setCompanyId(companyId);
                    newSchedule.setStartDate(date);
                    newSchedule.setPublished(false);
                    newSchedule.setDayOfWeek((short) date.getDayOfWeek().getValue());
                    newSchedule.setTimestamp(LocalDateTime.now());
                    return scheduleRepository.save(newSchedule);
                });
    }

    private void applyDerivedShiftFields(Shift shift, Float duration) {
        if (shift.getStartTime() != null && shift.getEndTime() != null) {
            shift.setDuration(duration != null
                    ? duration
                    : calculateDurationHours(shift.getStartTime(), shift.getEndTime()));
            shift.setIsOvernight(shift.getEndTime().isBefore(shift.getStartTime()));
        }
    }

    private List<EmployeeShiftProjection> findEmployeeShiftRows(Integer companyId, LocalDate startDate, LocalDate endDate) {
        return schedulingQueryRepository.findAllEmployeeShiftsInRange(
                companyId,
                startDate.minusDays(1),
                endDate
        );
    }

    private boolean hasShiftData(EmployeeShiftProjection row) {
        return row.getWeekCommencing() != null && row.getStartTime() != null && row.getEndTime() != null;
    }

    private boolean isWithinRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private EmployeeWithShiftsDto getOrCreateEmployeeDto(
            Map<Integer, EmployeeWithShiftsDto> grouped,
            EmployeeShiftProjection row,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return grouped.computeIfAbsent(
                row.getEmployeeId(),
                id -> new EmployeeWithShiftsDto(
                        row.getEmployeeId(),
                        row.getFirstName(),
                        row.getLastName(),
                        row.getPhones(),
                        row.getAvailablePositions(),
                        startDate,
                        endDate
                )
        );
    }

    private void addShiftSegment(EmployeeWithShiftsDto employeeDto, LocalDate rangeStartDate, ShiftSegment segment) {
        int dayIndex = Math.toIntExact(ChronoUnit.DAYS.between(rangeStartDate, segment.date()));
        employeeDto.addShiftToDay(dayIndex, segment.date(), new ShiftDto(
                segment.shiftId(),
                segment.startTime(),
                segment.endTime(),
                segment.position(),
                segment.category(),
                segment.description(),
                segment.durationHours(),
                segment.color()
        ));
        employeeDto.setTotalHours(employeeDto.getTotalHours() + segment.durationHours());
    }

    private List<ShiftSegment> buildShiftSegments(
            EmployeeShiftProjection row,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (!hasShiftData(row)) {
            return List.of();
        }

        List<ShiftSegment> segments = new ArrayList<>();
        LocalDate scheduleDate = row.getWeekCommencing();

        if (Boolean.TRUE.equals(row.getIsOvernight())) {
            if (isWithinRange(scheduleDate, startDate, endDate)) {
                segments.add(createShiftSegment(
                        row,
                        scheduleDate,
                        row.getStartTime(),
                        LocalTime.MIDNIGHT,
                        calculateDurationHours(row.getStartTime(), LocalTime.MIDNIGHT)
                ));
            }

            LocalDate nextDay = scheduleDate.plusDays(1);
            if (isWithinRange(nextDay, startDate, endDate)) {
                segments.add(createShiftSegment(
                        row,
                        nextDay,
                        LocalTime.MIDNIGHT,
                        row.getEndTime(),
                        calculateDurationHours(LocalTime.MIDNIGHT, row.getEndTime())
                ));
            }

            return segments;
        }

        if (isWithinRange(scheduleDate, startDate, endDate)) {
            segments.add(createShiftSegment(
                    row,
                    scheduleDate,
                    row.getStartTime(),
                    row.getEndTime(),
                    resolveDuration(row)
            ));
        }

        return segments;
    }

    private ShiftSegment createShiftSegment(
            EmployeeShiftProjection row,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            float durationHours
    ) {
        return new ShiftSegment(
                row.getShiftId(),
                row.getEmployeeId(),
                row.getFirstName(),
                row.getLastName(),
                row.getPhones(),
                date,
                startTime,
                endTime,
                row.getPosition(),
                row.getCategory(),
                row.getDescription(),
                durationHours,
                row.getColor()
        );
    }

    private Map<LocalDate, DayPositionBucketDto> initializeDayBuckets(
            LocalDate startDate,
            LocalDate endDate,
            List<String> companyPositions
    ) {
        Map<LocalDate, DayPositionBucketDto> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            List<PositionShiftBucketDto> positions = new ArrayList<>();
            for (String position : companyPositions) {
                positions.add(new PositionShiftBucketDto(position, new ArrayList<>(), 0, 0.0f));
            }
            grouped.put(bucketDate, new DayPositionBucketDto(bucketDate, positions, 0, 0.0f));
        }

        return grouped;
    }

    private List<String> getCompanyPositionNames(Integer companyId) {
        if (companyId == null) {
            return List.of();
        }

        return positionService.getPositionsByCompanyId(companyId).stream()
                .map(PositionDto::getName)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private PositionShiftBucketDto getOrCreatePositionBucket(DayPositionBucketDto dayBucket, String position) {
        for (PositionShiftBucketDto positionBucket : dayBucket.positions()) {
            if (Objects.equals(positionBucket.position(), position)) {
                return positionBucket;
            }
        }

        PositionShiftBucketDto positionBucket = new PositionShiftBucketDto(position, new ArrayList<>(), 0, 0.0f);
        dayBucket.positions().add(positionBucket);
        return positionBucket;
    }

    private void updatePositionBucket(
            DayPositionBucketDto dayBucket,
            PositionShiftBucketDto positionBucket,
            float durationHours
    ) {
        int index = dayBucket.positions().indexOf(positionBucket);
        dayBucket.positions().set(
                index,
                new PositionShiftBucketDto(
                        positionBucket.position(),
                        positionBucket.shifts(),
                        positionBucket.shiftCount() + 1,
                        positionBucket.totalDuration() + durationHours
                )
        );
    }

    private float resolveDuration(EmployeeShiftProjection row) {
        return row.getDuration() != null
                ? row.getDuration()
                : calculateDurationHours(row.getStartTime(), row.getEndTime());
    }

    private float calculateDurationHours(LocalTime startTime, LocalTime endTime) {
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return minutes / 60.0f;
    }

    private record ShiftSegment(
            Integer shiftId,
            Integer employeeId,
            String firstName,
            String lastName,
            List<String> phones,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String position,
            String category,
            String description,
            Float durationHours,
            String color
    ) {
    }
}
