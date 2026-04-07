package com.w2w.api.scheduling;

import com.w2w.api.category.CategoryService;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.dto.CategoryTimingBucket;
import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.DayCategoryTimingBucket;
import com.w2w.api.scheduling.dto.DayPositionTimingBucket;
import com.w2w.api.scheduling.dto.DayShiftTimingBucket;
import com.w2w.api.scheduling.dto.EmployeeScheduledShift;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.GroupedShiftDate;
import com.w2w.api.scheduling.dto.GroupedShiftsResponse;
import com.w2w.api.scheduling.dto.PositionTimingBucket;
import com.w2w.api.scheduling.dto.ShiftGroup;
import com.w2w.api.scheduling.dto.ShiftGrouping;
import com.w2w.api.scheduling.dto.ShiftTimingBucket;
import com.w2w.api.scheduling.dto.ShiftTimingGroup;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.DayPositionBucket;
import com.w2w.api.scheduling.dto.EmployeeSchedule;
import com.w2w.api.scheduling.dto.EmployeeShift;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.PositionShiftBucket;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.ShiftResponse;
import com.w2w.api.scheduling.dto.ShiftSummary;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SchedulingService {
    private static final DateTimeFormatter SHIFT_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma");

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

    @Autowired
    private CategoryService categoryService;

    public ShiftResponse getShift(Integer shiftId) {
        ShiftDetailsProjection shift = shiftRepository.findShiftDetailsByShiftId(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));

        return new ShiftResponse(
                shift.getShiftId(),
                shift.getEmployeeId(),
                shift.getCompanyId(),
                shift.getDescription(),
                shift.getDate(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getDuration(),
                shift.getIsOvernight(),
                shift.getPosition(),
                shift.getCategory(),
                shift.getColor()
        );
    }

    public Shift saveShift(CreateShiftRequest request) {
        Shift shift = new Shift();
        shift.setEmployeeId(request.employeeId());
        shift.setCompanyId(request.companyId());
        shift.setDescription(request.description());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setRequiredPositionId(request.position());
        shift.setCategoryId(request.category());
        shift.setColor(request.color());
        shift.setIsDeleted(false);

        if (request.date() != null) {
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.date());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.duration());
        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public ShiftResponse updateShift(Integer shiftId, UpdateShiftRequest request) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));

        if (request.employeeId() != null) shift.setEmployeeId(request.employeeId());
        if (request.description() != null) shift.setDescription(request.description());
        if (request.startTime() != null) shift.setStartTime(request.startTime());
        if (request.endTime() != null) shift.setEndTime(request.endTime());
        if (request.position() != null) shift.setRequiredPositionId(request.position());
        if (request.category() != null) shift.setCategoryId(request.category());
        if (request.color() != null) shift.setColor(request.color());

        if (request.date() != null) {
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.date());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.duration());
        shift.setChangedBy(shift.getEmployeeId());
        shiftRepository.save(shift);
        return getShift(shiftId);
    }

    public void softDeleteShift(Integer shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));
        shift.setIsDeleted(true);
        shiftRepository.save(shift);
    }

    public List<EmployeeSchedule> getEmployeeShiftsGroupedInRange(Integer companyId, LocalDate startDate, LocalDate endDate) {
        List<EmployeeShiftProjection> flatResults = findEmployeeShiftRows(companyId, startDate, endDate);
        Map<Integer, EmployeeSchedule> grouped = new LinkedHashMap<>();

        for (EmployeeShiftProjection row : flatResults) {
            EmployeeSchedule employeeDto = getOrCreateEmployeeDto(grouped, row, startDate, endDate);

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

    public GroupedShiftsResponse getShiftsGrouped(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            ShiftGrouping grouping
    ) {
        List<GroupedShiftDate> dates = switch (grouping) {
            case POSITION_SHIFT_TIMINGS -> toGroupedDatesFromPositionTimingBuckets(
                    getShiftsGroupedByDayPositionAndTiming(companyId, startDate, endDate)
            );
            case SHIFT_TIMINGS -> toGroupedDatesFromDayTimingBuckets(
                    getShiftsGroupedByDayAndTiming(companyId, startDate, endDate)
            );
            case CATEGORY_SHIFT_TIMINGS -> toGroupedDatesFromCategoryTimingBuckets(
                    getShiftsGroupedByDayCategoryAndTiming(companyId, startDate, endDate)
            );
            case CAT_SHIFT_TIMINGS -> toGroupedDatesFromCategoryTimingBuckets(
                    getShiftsGroupedByDayCategoryShortNameAndTiming(companyId, startDate, endDate)
            );
        };
        return new GroupedShiftsResponse(dates);
    }

    public List<DayPositionBucket> getShiftsGroupedByDateAndPosition(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<Integer, DayPositionBucket> grouped = initializeDayBuckets(
                startDate,
                endDate,
                getCompanyPositionNames(companyId)
        );
        List<ShiftSegment> segments = findGroupedShiftSegments(companyId, startDate, endDate);

        for (ShiftSegment segment : segments) {
            int dayIndex = Math.toIntExact(ChronoUnit.DAYS.between(startDate, segment.date()));
            DayPositionBucket dayBucket = grouped.get(dayIndex);
            PositionShiftBucket positionBucket = getOrCreatePositionBucket(dayBucket, segment.position());
            positionBucket.shifts().add(new EmployeeShift(
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
            updateDayPositionBucket(dayBucket, positionBucket, segment.durationHours());
            grouped.put(
                    dayIndex,
                    new DayPositionBucket(
                            dayBucket.date(),
                            dayBucket.positions(),
                            dayBucket.shiftCount() + 1,
                            dayBucket.totalDuration() + segment.durationHours()
                    )
            );
        }

        return new ArrayList<>(grouped.values());
    }

    public List<DayPositionTimingBucket> getShiftsGroupedByDayPositionAndTiming(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DayPositionTimingBucket> grouped = initializeDayPositionTimingBuckets(
                startDate,
                endDate,
                getCompanyPositionNames(companyId)
        );
        List<ShiftSegment> segments = findGroupedShiftSegments(companyId, startDate, endDate);

        for (ShiftSegment segment : segments) {
            DayPositionTimingBucket dayBucket = grouped.get(segment.date());
            PositionTimingBucket positionBucket = getOrCreatePositionTimingBucket(dayBucket, segment.position());
            ShiftTimingBucket shiftTimingBucket = getOrCreateShiftTimingBucket(
                    positionBucket,
                    segment.startTime(),
                    segment.endTime()
            );
            shiftTimingBucket.shifts().add(new EmployeeScheduledShift(
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
            updateShiftTimingBucket(positionBucket, shiftTimingBucket, segment.durationHours());
            updatePositionTimingBucket(dayBucket, positionBucket, segment.durationHours());
            grouped.put(
                    segment.date(),
                    new DayPositionTimingBucket(
                            dayBucket.date(),
                            dayBucket.positions(),
                            dayBucket.shiftCount() + 1,
                            dayBucket.totalDuration() + segment.durationHours()
                    )
            );
        }

        return new ArrayList<>(grouped.values());
    }

    public List<DayCategoryTimingBucket> getShiftsGroupedByDayCategoryAndTiming(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getShiftsGroupedByDayCategoryAndTiming(
                companyId,
                startDate,
                endDate,
                buildCategoryLabelByName(companyId, false)
        );
    }

    public List<DayCategoryTimingBucket> getShiftsGroupedByDayCategoryShortNameAndTiming(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getShiftsGroupedByDayCategoryAndTiming(
                companyId,
                startDate,
                endDate,
                buildCategoryLabelByName(companyId, true)
        );
    }

    private List<DayCategoryTimingBucket> getShiftsGroupedByDayCategoryAndTiming(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            Map<String, String> categoryLabelByName
    ) {
        Map<LocalDate, DayCategoryTimingBucket> grouped = initializeDayCategoryTimingBuckets(
                startDate,
                endDate,
                categoryLabelByName.values().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList()
        );
        List<ShiftSegment> segments = findGroupedShiftSegments(companyId, startDate, endDate);

        for (ShiftSegment segment : segments) {
            DayCategoryTimingBucket dayBucket = grouped.get(segment.date());
            CategoryTimingBucket categoryBucket = getOrCreateCategoryTimingBucket(
                    dayBucket,
                    resolveCategoryLabel(categoryLabelByName, segment.category())
            );
            ShiftTimingGroup shiftTimingBucket = getOrCreateShiftTimingGroup(
                    categoryBucket,
                    segment.startTime(),
                    segment.endTime()
            );
            shiftTimingBucket.shifts().add(new EmployeeScheduledShift(
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
        }

        return new ArrayList<>(grouped.values());
    }

    public List<DayShiftTimingBucket> getShiftsGroupedByDayAndTiming(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DayShiftTimingBucket> grouped = initializeDayShiftTimingBuckets(startDate, endDate);
        List<ShiftSegment> segments = findDayTimingShiftSegments(companyId, startDate, endDate);

        for (ShiftSegment segment : segments) {
            DayShiftTimingBucket dayBucket = grouped.get(segment.date());
            ShiftTimingGroup shiftTimingGroup = getOrCreateShiftTimingGroup(
                    dayBucket,
                    segment.startTime(),
                    segment.endTime()
            );
            shiftTimingGroup.shifts().add(new EmployeeScheduledShift(
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
        }

        return new ArrayList<>(grouped.values());
    }

    private List<ShiftSegment> findGroupedShiftSegments(Integer companyId, LocalDate startDate, LocalDate endDate) {
        List<ShiftSegment> segments = new ArrayList<>();

        for (EmployeeShiftProjection row : findEmployeeShiftRows(companyId, startDate, endDate)) {
            segments.addAll(buildShiftSegments(row, startDate, endDate));
        }

        segments.sort(Comparator
                .comparing(ShiftSegment::date)
                .thenComparing(segment -> segment.position() == null ? "" : segment.position())
                .thenComparing(ShiftSegment::startTime)
                .thenComparing(ShiftSegment::employeeId));
        return segments;
    }

    private List<ShiftSegment> findDayTimingShiftSegments(Integer companyId, LocalDate startDate, LocalDate endDate) {
        List<ShiftSegment> segments = new ArrayList<>();

        for (EmployeeShiftProjection row : findEmployeeShiftRows(companyId, startDate, endDate)) {
            segments.addAll(buildShiftSegments(row, startDate, endDate));
        }

        segments.sort(Comparator
                .comparing(ShiftSegment::date)
                .thenComparing(ShiftSegment::startTime)
                .thenComparing(ShiftSegment::endTime)
                .thenComparing(ShiftSegment::employeeId));
        return segments;
    }

    private List<GroupedShiftDate> toGroupedDatesFromPositionTimingBuckets(List<DayPositionTimingBucket> dateBuckets) {
        return dateBuckets.stream()
                .map(dateBucket -> new GroupedShiftDate(
                        dateBucket.date(),
                        dateBucket.positions().stream()
                                .map(this::toPositionTimingShiftGroup)
                                .toList()
                ))
                .toList();
    }

    private List<GroupedShiftDate> toGroupedDatesFromCategoryTimingBuckets(List<DayCategoryTimingBucket> dateBuckets) {
        return dateBuckets.stream()
                .map(dateBucket -> new GroupedShiftDate(
                        dateBucket.date(),
                        dateBucket.categories().stream()
                                .map(this::toCategoryTimingShiftGroup)
                                .toList()
                ))
                .toList();
    }

    private List<GroupedShiftDate> toGroupedDatesFromDayTimingBuckets(List<DayShiftTimingBucket> dateBuckets) {
        return dateBuckets.stream()
                .map(dateBucket -> new GroupedShiftDate(
                        dateBucket.date(),
                        dateBucket.shiftTimings().stream()
                                .map(this::toTimingShiftGroup)
                                .toList()
                ))
                .toList();
    }

    private ShiftGroup toPositionTimingShiftGroup(PositionTimingBucket positionBucket) {
        return new ShiftGroup(
                positionBucket.position(),
                positionBucket.shiftTimings().stream()
                        .map(this::toTimingShiftGroup)
                        .toList(),
                List.of()
        );
    }

    private ShiftGroup toCategoryTimingShiftGroup(CategoryTimingBucket categoryBucket) {
        return new ShiftGroup(
                categoryBucket.category(),
                categoryBucket.shiftTimings().stream()
                        .map(this::toTimingShiftGroup)
                        .toList(),
                List.of()
        );
    }

    private ShiftGroup toTimingShiftGroup(ShiftTimingBucket shiftTimingBucket) {
        return new ShiftGroup(
                formatShiftTimingLabel(shiftTimingBucket.startTime(), shiftTimingBucket.endTime()),
                List.of(),
                shiftTimingBucket.shifts()
        );
    }

    private ShiftGroup toTimingShiftGroup(ShiftTimingGroup shiftTimingGroup) {
        return new ShiftGroup(
                shiftTimingGroup.label(),
                List.of(),
                shiftTimingGroup.shifts()
        );
    }

    public List<ConflictItem> validate(FindConflictRequest request) {
        List<ConflictItem> conflicts = new ArrayList<>();
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

    private EmployeeSchedule getOrCreateEmployeeDto(
            Map<Integer, EmployeeSchedule> grouped,
            EmployeeShiftProjection row,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return grouped.computeIfAbsent(
                row.getEmployeeId(),
                id -> new EmployeeSchedule(
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

    private void addShiftSegment(EmployeeSchedule employeeDto, LocalDate rangeStartDate, ShiftSegment segment) {
        int dayIndex = Math.toIntExact(ChronoUnit.DAYS.between(rangeStartDate, segment.date()));
        employeeDto.addShiftToDay(dayIndex, segment.date(), new ShiftSummary(
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

    private Map<Integer, DayPositionBucket> initializeDayBuckets(
            LocalDate startDate,
            LocalDate endDate,
            List<String> companyPositions
    ) {
        Map<Integer, DayPositionBucket> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            List<PositionShiftBucket> positions = new ArrayList<>();
            for (String position : companyPositions) {
                positions.add(new PositionShiftBucket(position, new ArrayList<>(), 0, 0.0f));
            }
            grouped.put(i, new DayPositionBucket(bucketDate, positions, 0, 0.0f));
        }

        return grouped;
    }

    private Map<LocalDate, DayPositionTimingBucket> initializeDayPositionTimingBuckets(
            LocalDate startDate,
            LocalDate endDate,
            List<String> companyPositions
    ) {
        Map<LocalDate, DayPositionTimingBucket> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            List<PositionTimingBucket> positions = new ArrayList<>();
            for (String position : companyPositions) {
                positions.add(new PositionTimingBucket(position, new ArrayList<>(), 0, 0.0f));
            }
            grouped.put(bucketDate, new DayPositionTimingBucket(bucketDate, positions, 0, 0.0f));
        }

        return grouped;
    }

    private Map<LocalDate, DayCategoryTimingBucket> initializeDayCategoryTimingBuckets(
            LocalDate startDate,
            LocalDate endDate,
            List<String> companyCategories
    ) {
        Map<LocalDate, DayCategoryTimingBucket> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            List<CategoryTimingBucket> categories = new ArrayList<>();
            for (String category : companyCategories) {
                categories.add(new CategoryTimingBucket(category, new ArrayList<>()));
            }
            grouped.put(bucketDate, new DayCategoryTimingBucket(bucketDate, categories));
        }

        return grouped;
    }

    private Map<LocalDate, DayShiftTimingBucket> initializeDayShiftTimingBuckets(
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DayShiftTimingBucket> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            grouped.put(bucketDate, new DayShiftTimingBucket(bucketDate, new ArrayList<>()));
        }

        return grouped;
    }

    private List<String> getCompanyPositionNames(Integer companyId) {
        if (companyId == null) {
            return List.of();
        }

        return positionService.getPositions(companyId, "all").stream()
                .map(PositionSummary::description)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private PositionShiftBucket getOrCreatePositionBucket(DayPositionBucket dayBucket, String position) {
        for (PositionShiftBucket positionBucket : dayBucket.positions()) {
            if (Objects.equals(positionBucket.position(), position)) {
                return positionBucket;
            }
        }

        PositionShiftBucket positionBucket = new PositionShiftBucket(position, new ArrayList<>(), 0, 0.0f);
        dayBucket.positions().add(positionBucket);
        return positionBucket;
    }

    private Map<String, String> buildCategoryLabelByName(Integer companyId, boolean useShortName) {
        if (companyId == null) {
            return Map.of();
        }

        Map<String, String> labels = new LinkedHashMap<>();
        for (CategorySummary category : categoryService.getCategoriesByCompanyId(companyId)) {
            if (category.name() == null) {
                continue;
            }
            labels.put(category.name(), useShortName ? preferredCategoryLabel(category) : category.name());
        }
        return labels;
    }

    private String preferredCategoryLabel(CategorySummary category) {
        if (category.shortName() != null && !category.shortName().isBlank()) {
            return category.shortName();
        }
        return category.name();
    }

    private String resolveCategoryLabel(Map<String, String> categoryLabelByName, String categoryName) {
        return categoryLabelByName.getOrDefault(categoryName, categoryName);
    }

    private PositionTimingBucket getOrCreatePositionTimingBucket(DayPositionTimingBucket dayBucket, String position) {
        for (PositionTimingBucket positionBucket : dayBucket.positions()) {
            if (Objects.equals(positionBucket.position(), position)) {
                return positionBucket;
            }
        }

        PositionTimingBucket positionBucket = new PositionTimingBucket(position, new ArrayList<>(), 0, 0.0f);
        dayBucket.positions().add(positionBucket);
        return positionBucket;
    }

    private CategoryTimingBucket getOrCreateCategoryTimingBucket(DayCategoryTimingBucket dayBucket, String category) {
        for (CategoryTimingBucket categoryBucket : dayBucket.categories()) {
            if (Objects.equals(categoryBucket.category(), category)) {
                return categoryBucket;
            }
        }

        CategoryTimingBucket categoryBucket = new CategoryTimingBucket(category, new ArrayList<>());
        dayBucket.categories().add(categoryBucket);
        return categoryBucket;
    }

    private ShiftTimingBucket getOrCreateShiftTimingBucket(
            PositionTimingBucket positionBucket,
            LocalTime startTime,
            LocalTime endTime
    ) {
        for (ShiftTimingBucket shiftTimingBucket : positionBucket.shiftTimings()) {
            if (Objects.equals(shiftTimingBucket.startTime(), startTime)
                    && Objects.equals(shiftTimingBucket.endTime(), endTime)) {
                return shiftTimingBucket;
            }
        }

        ShiftTimingBucket shiftTimingBucket = new ShiftTimingBucket(
                startTime,
                endTime,
                new ArrayList<>(),
                0,
                0.0f
        );
        positionBucket.shiftTimings().add(shiftTimingBucket);
        return shiftTimingBucket;
    }

    private ShiftTimingGroup getOrCreateShiftTimingGroup(
            CategoryTimingBucket categoryBucket,
            LocalTime startTime,
            LocalTime endTime
    ) {
        String label = formatShiftTimingLabel(startTime, endTime);
        for (ShiftTimingGroup shiftTimingBucket : categoryBucket.shiftTimings()) {
            if (Objects.equals(shiftTimingBucket.label(), label)) {
                return shiftTimingBucket;
            }
        }

        ShiftTimingGroup shiftTimingBucket = new ShiftTimingGroup(label, new ArrayList<>());
        categoryBucket.shiftTimings().add(shiftTimingBucket);
        return shiftTimingBucket;
    }

    private ShiftTimingGroup getOrCreateShiftTimingGroup(
            DayShiftTimingBucket dayBucket,
            LocalTime startTime,
            LocalTime endTime
    ) {
        String label = formatShiftTimingLabel(startTime, endTime);
        for (ShiftTimingGroup shiftTimingBucket : dayBucket.shiftTimings()) {
            if (Objects.equals(shiftTimingBucket.label(), label)) {
                return shiftTimingBucket;
            }
        }

        ShiftTimingGroup shiftTimingBucket = new ShiftTimingGroup(label, new ArrayList<>());
        dayBucket.shiftTimings().add(shiftTimingBucket);
        return shiftTimingBucket;
    }

    private void updateShiftTimingBucket(
            PositionTimingBucket positionBucket,
            ShiftTimingBucket shiftTimingBucket,
            float durationHours
    ) {
        int index = positionBucket.shiftTimings().indexOf(shiftTimingBucket);
        positionBucket.shiftTimings().set(
                index,
                new ShiftTimingBucket(
                        shiftTimingBucket.startTime(),
                        shiftTimingBucket.endTime(),
                        shiftTimingBucket.shifts(),
                        shiftTimingBucket.shiftCount() + 1,
                        shiftTimingBucket.totalDuration() + durationHours
                )
        );
    }

    private void updateDayPositionBucket(
            DayPositionBucket dayBucket,
            PositionShiftBucket positionBucket,
            float durationHours
    ) {
        int index = dayBucket.positions().indexOf(positionBucket);
        dayBucket.positions().set(
                index,
                new PositionShiftBucket(
                        positionBucket.position(),
                        positionBucket.shifts(),
                        positionBucket.shiftCount() + 1,
                        positionBucket.totalDuration() + durationHours
                )
        );
    }

    private void updatePositionTimingBucket(
            DayPositionTimingBucket dayBucket,
            PositionTimingBucket positionBucket,
            float durationHours
    ) {
        int index = dayBucket.positions().indexOf(positionBucket);
        dayBucket.positions().set(
                index,
                new PositionTimingBucket(
                        positionBucket.position(),
                        positionBucket.shiftTimings(),
                        positionBucket.shiftCount() + 1,
                        positionBucket.totalDuration() + durationHours
                )
        );
    }

    private String formatShiftTimingLabel(LocalTime startTime, LocalTime endTime) {
        return startTime.format(SHIFT_TIME_FORMATTER) + "-" + endTime.format(SHIFT_TIME_FORMATTER);
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
