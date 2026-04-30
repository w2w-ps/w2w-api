package com.w2w.api.scheduling;

import com.w2w.api.category.CategoryService;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.config.TenantContext;
import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.*;

@Service
public class SchedulingGroupingService {
    private static final DateTimeFormatter SHORT_SHIFT_TIME_FORMATTER = DateTimeFormatter.ofPattern("ha", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_SHIFT_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_BUCKET_WEEKDAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
    private static final DateTimeFormatter TITLE_SINGLE_DAY_FORMATTER = DateTimeFormatter.ofPattern("EEEE - MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter WEEK_TEXT_FORMATTER = DateTimeFormatter.ofPattern("MMM-dd", Locale.ENGLISH);

    private final SchedulingQueryRepository schedulingQueryRepository;
    private final PositionService positionService;
    private final CategoryService categoryService;

    public SchedulingGroupingService(
            SchedulingQueryRepository schedulingQueryRepository,
            PositionService positionService,
            CategoryService categoryService
    ) {
        this.schedulingQueryRepository = schedulingQueryRepository;
        this.positionService = positionService;
        this.categoryService = categoryService;
    }

    public List<EmployeeSchedule> getEmployeeShiftsGroupedInRange(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        Set<Integer> positionFilter = toFilterSet(positionIds);
        Set<Integer> categoryFilter = toFilterSet(categoryIds);
        List<EmployeeShiftProjection> flatResults = findEmployeeShiftRows(
                TenantContext.getCurrentTenant(),
                startDate,
                endDate,
                positionFilter,
                categoryFilter
        );
        boolean hasFilters = !positionFilter.isEmpty() || !categoryFilter.isEmpty();
        Map<Integer, EmployeeSchedule> grouped = new LinkedHashMap<>();

        for (EmployeeShiftProjection row : flatResults) {
            List<ShiftSegment> segments = buildShiftSegments(row, startDate, endDate);
            List<ShiftSegment> matchingSegments = segments.stream()
                    .filter(segment -> matchesFilters(segment, positionFilter, categoryFilter))
                    .toList();

            if (matchingSegments.isEmpty()) {
                if (!hasFilters && segments.isEmpty()) {
                    getOrCreateEmployeeDto(grouped, row, startDate, endDate);
                }
                continue;
            }

            EmployeeSchedule employeeDto = getOrCreateEmployeeDto(grouped, row, startDate, endDate);
            updatePublishedStage(employeeDto, row);

            for (ShiftSegment segment : matchingSegments) {
                addShiftSegment(employeeDto, startDate, segment);
            }

            employeeDto.setShiftCount(employeeDto.getShiftCount() + 1);
        }

        return new ArrayList<>(grouped.values());
    }

    public GroupedShiftsResponse getShiftsGrouped(
            LocalDate startDate,
            LocalDate endDate,
            ShiftGrouping grouping
    ) {
        return getShiftsGrouped(startDate, endDate, grouping, null, null);
    }

    public GroupedShiftsResponse getShiftsGrouped(
            LocalDate startDate,
            LocalDate endDate,
            ShiftGrouping grouping,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        return switch (grouping) {
            case POSITION_SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromPositionTimingBuckets(
                    getShiftsGroupedByDayPositionAndTiming(startDate, endDate, positionIds, categoryIds)
            ));
            case SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromDayTimingBuckets(
                    getShiftsGroupedByDayAndTiming(startDate, endDate, positionIds, categoryIds)
            ));
            case CATEGORY_SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromCategoryTimingBuckets(
                    getShiftsGroupedByDayCategoryAndTiming(startDate, endDate, positionIds, categoryIds)
            ));
            case CAT_SHIFT_TIMINGS -> new GroupedShiftsResponse(toGroupedDatesFromCategoryTimingBuckets(
                    getShiftsGroupedByDayCategoryShortNameAndTiming(startDate, endDate, positionIds, categoryIds)
            ));
        };
    }

    public DatePositionSummaryResponse getShiftsGroupedByDateAndPosition(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        Integer companyId = TenantContext.getCurrentTenant();
        Set<Integer> positionFilter = toFilterSet(positionIds);
        Set<Integer> categoryFilter = toFilterSet(categoryIds);
        Map<Integer, DayPositionBucket> grouped = initializeDayBuckets(
                startDate,
                endDate,
                getCompanyPositionNames(companyId, positionFilter)
        );
        List<ShiftSegment> segments = findGroupedShiftSegments(companyId, startDate, endDate, positionFilter, categoryFilter);
        int totalShifts = 0;
        BigDecimal totalHours = scaledHours(0.0f);

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
                    null,
                    formatTime(segment.startTime()),
                    formatTime(segment.endTime()),
                    segment.categoryShortDescription(),
                    segment.description(),
                    segment.durationHours(),
                    segment.color()
            ));
            updateDayPositionBucket(dayBucket, positionBucket, segment.durationHours());
            grouped.put(
                    dayIndex,
                    new DayPositionBucket(
                            dayBucket.weekday(),
                            dayBucket.date(),
                            dayBucket.positions(),
                            dayBucket.shiftCount() + 1,
                            addHours(dayBucket.totalDuration(), segment.durationHours())
                    )
            );
            totalShifts++;
            totalHours = addHours(totalHours, segment.durationHours());
        }

        return new DatePositionSummaryResponse(
                formatTitle(startDate, endDate),
                totalShifts,
                totalHours,
                new ArrayList<>(grouped.values())
        );
    }

    public List<DayPositionTimingBucketDto> getShiftsGroupedByDayPositionAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getShiftsGroupedByDayPositionAndTiming(startDate, endDate, null, null);
    }

    public List<DayPositionTimingBucketDto> getShiftsGroupedByDayPositionAndTiming(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        Integer companyId = TenantContext.getCurrentTenant();
        Set<Integer> positionFilter = toFilterSet(positionIds);
        Set<Integer> categoryFilter = toFilterSet(categoryIds);
        Map<LocalDate, DayPositionTimingBucketDto> grouped = initializeDayPositionTimingBuckets(
                startDate,
                endDate,
                getCompanyPositionNames(companyId, positionFilter)
        );
        List<ShiftSegment> segments = findGroupedShiftSegments(companyId, startDate, endDate, positionFilter, categoryFilter);

        for (ShiftSegment segment : segments) {
            DayPositionTimingBucketDto dayBucket = grouped.get(segment.date());
            PositionTimingBucketDto positionBucket = getOrCreatePositionTimingBucket(dayBucket, segment.position());
            ShiftTimingBucketDto shiftTimingBucket = getOrCreateShiftTimingBucket(
                    positionBucket,
                    segment.startTime(),
                    segment.endTime()
            );
            shiftTimingBucket.shifts().add(new EmployeeScheduledShiftDto(
                    segment.shiftId(),
                    segment.employeeId(),
                    segment.firstName(),
                    segment.lastName(),
                    segment.phones(),
                    segment.startTime(),
                    segment.endTime(),
                    segment.position(),
                    segment.category(),
                    segment.description(),
                    segment.durationHours(),
                    segment.color()
            ));
            updateShiftTimingBucket(positionBucket, shiftTimingBucket, segment.durationHours());
            updatePositionTimingBucket(dayBucket, positionBucket, segment.durationHours());
            grouped.put(
                    segment.date(),
                    new DayPositionTimingBucketDto(
                            dayBucket.date(),
                            dayBucket.positions(),
                            dayBucket.shiftCount() + 1,
                            dayBucket.totalDuration() + segment.durationHours()
                    )
            );
        }

        return new ArrayList<>(grouped.values());
    }

    public List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getShiftsGroupedByDayCategoryAndTiming(startDate, endDate, null, null);
    }

    public List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryAndTiming(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        Integer companyId = TenantContext.getCurrentTenant();
        Set<Integer> categoryFilter = toFilterSet(categoryIds);
        return getShiftsGroupedByDayCategoryAndTiming(
                startDate,
                endDate,
                companyId,
                buildCategoryLabelByName(companyId, false, categoryFilter),
                toFilterSet(positionIds),
                categoryFilter
        );
    }

    public List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryShortNameAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getShiftsGroupedByDayCategoryShortNameAndTiming(startDate, endDate, null, null);
    }

    public List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryShortNameAndTiming(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        Integer companyId = TenantContext.getCurrentTenant();
        Set<Integer> categoryFilter = toFilterSet(categoryIds);
        return getShiftsGroupedByDayCategoryAndTiming(
                startDate,
                endDate,
                companyId,
                buildCategoryLabelByName(companyId, true, categoryFilter),
                toFilterSet(positionIds),
                categoryFilter
        );
    }

    private List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryAndTiming(
            LocalDate startDate,
            LocalDate endDate,
            Integer companyId,
            Map<String, String> categoryLabelByName,
            Set<Integer> positionIds,
            Set<Integer> categoryIds
    ) {
        Map<LocalDate, DayCategoryTimingBucketDto> grouped = initializeDayCategoryTimingBuckets(
                startDate,
                endDate,
                categoryLabelByName.values().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .toList()
        );
        List<ShiftSegment> segments = findGroupedShiftSegments(companyId, startDate, endDate, positionIds, categoryIds);

        for (ShiftSegment segment : segments) {
            DayCategoryTimingBucketDto dayBucket = grouped.get(segment.date());
            CategoryTimingBucketDto categoryBucket = getOrCreateCategoryTimingBucket(
                    dayBucket,
                    resolveCategoryLabel(categoryLabelByName, segment.category())
            );
            ShiftTimingGroupDto shiftTimingBucket = getOrCreateShiftTimingGroup(
                    categoryBucket,
                    segment.startTime(),
                    segment.endTime()
            );
            shiftTimingBucket.shifts().add(new EmployeeScheduledShiftDto(
                    segment.shiftId(),
                    segment.employeeId(),
                    segment.firstName(),
                    segment.lastName(),
                    segment.phones(),
                    segment.startTime(),
                    segment.endTime(),
                    segment.position(),
                    segment.category(),
                    segment.description(),
                    segment.durationHours(),
                    segment.color()
            ));
        }

        return new ArrayList<>(grouped.values());
    }

    public List<DayShiftTimingBucketDto> getShiftsGroupedByDayAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return getShiftsGroupedByDayAndTiming(startDate, endDate, null, null);
    }

    public List<DayShiftTimingBucketDto> getShiftsGroupedByDayAndTiming(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        Integer companyId = TenantContext.getCurrentTenant();
        Map<LocalDate, DayShiftTimingBucketDto> grouped = initializeDayShiftTimingBuckets(startDate, endDate);
        List<ShiftSegment> segments = findDayTimingShiftSegments(
                companyId,
                startDate,
                endDate,
                toFilterSet(positionIds),
                toFilterSet(categoryIds)
        );

        for (ShiftSegment segment : segments) {
            DayShiftTimingBucketDto dayBucket = grouped.get(segment.date());
            ShiftTimingGroupDto shiftTimingGroup = getOrCreateShiftTimingGroup(
                    dayBucket,
                    segment.startTime(),
                    segment.endTime()
            );
            shiftTimingGroup.shifts().add(new EmployeeScheduledShiftDto(
                    segment.shiftId(),
                    segment.employeeId(),
                    segment.firstName(),
                    segment.lastName(),
                    segment.phones(),
                    segment.startTime(),
                    segment.endTime(),
                    segment.position(),
                    segment.category(),
                    segment.description(),
                    segment.durationHours(),
                    segment.color()
            ));
        }

        return new ArrayList<>(grouped.values());
    }

    private List<ShiftSegment> findGroupedShiftSegments(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            Set<Integer> positionIds,
            Set<Integer> categoryIds
    ) {
        List<ShiftSegment> segments = new ArrayList<>();

        for (EmployeeShiftProjection row : findEmployeeShiftRows(companyId, startDate, endDate, positionIds, categoryIds)) {
            for (ShiftSegment segment : buildShiftSegments(row, startDate, endDate)) {
                if (matchesFilters(segment, positionIds, categoryIds)) {
                    segments.add(segment);
                }
            }
        }

        segments.sort(Comparator
                .comparing(ShiftSegment::date)
                .thenComparing(segment -> segment.position() == null ? "" : segment.position())
                .thenComparing(segment -> segment.lastName() == null ? "" : segment.lastName())
                .thenComparing(segment -> segment.firstName() == null ? "" : segment.firstName())
                .thenComparing(ShiftSegment::startTime)
                .thenComparing(ShiftSegment::employeeId));
        return segments;
    }

    private List<ShiftSegment> findDayTimingShiftSegments(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            Set<Integer> positionIds,
            Set<Integer> categoryIds
    ) {
        List<ShiftSegment> segments = new ArrayList<>();

        for (EmployeeShiftProjection row : findEmployeeShiftRows(companyId, startDate, endDate, positionIds, categoryIds)) {
            for (ShiftSegment segment : buildShiftSegments(row, startDate, endDate)) {
                if (matchesFilters(segment, positionIds, categoryIds)) {
                    segments.add(segment);
                }
            }
        }

        segments.sort(Comparator
                .comparing(ShiftSegment::date)
                .thenComparing(ShiftSegment::startTime)
                .thenComparing(ShiftSegment::endTime)
                .thenComparing(ShiftSegment::employeeId));
        return segments;
    }

    private List<GroupedShiftDate> toGroupedDatesFromPositionTimingBuckets(List<DayPositionTimingBucketDto> dateBuckets) {
        return dateBuckets.stream()
                .map(dateBucket -> new GroupedShiftDate(
                        dateBucket.date(),
                        dateBucket.positions().stream()
                                .map(this::toPositionTimingShiftGroup)
                                .toList()
                ))
                .toList();
    }

    private List<GroupedShiftDate> toGroupedDatesFromCategoryTimingBuckets(List<DayCategoryTimingBucketDto> dateBuckets) {
        return dateBuckets.stream()
                .map(dateBucket -> new GroupedShiftDate(
                        dateBucket.date(),
                        dateBucket.categories().stream()
                                .map(this::toCategoryTimingShiftGroup)
                                .toList()
                ))
                .toList();
    }

    private List<GroupedShiftDate> toGroupedDatesFromDayTimingBuckets(List<DayShiftTimingBucketDto> dateBuckets) {
        return dateBuckets.stream()
                .map(dateBucket -> new GroupedShiftDate(
                        dateBucket.date(),
                        dateBucket.shiftTimings().stream()
                                .map(this::toTimingShiftGroup)
                                .toList()
                ))
                .toList();
    }

    private ShiftGroup toPositionTimingShiftGroup(PositionTimingBucketDto positionBucket) {
        return new ShiftGroup(
                positionBucket.position(),
                positionBucket.shiftTimings().stream()
                        .map(this::toTimingShiftGroup)
                        .toList(),
                List.of()
        );
    }

    private ShiftGroup toCategoryTimingShiftGroup(CategoryTimingBucketDto categoryBucket) {
        return new ShiftGroup(
                categoryBucket.category(),
                categoryBucket.shiftTimings().stream()
                        .map(this::toTimingShiftGroup)
                        .toList(),
                List.of()
        );
    }

    private ShiftGroup toTimingShiftGroup(ShiftTimingBucketDto shiftTimingBucket) {
        return new ShiftGroup(
                formatShiftTimingLabel(shiftTimingBucket.startTime(), shiftTimingBucket.endTime()),
                List.of(),
                shiftTimingBucket.shifts().stream().map(this::toEmployeeScheduledShift).toList()
        );
    }

    private ShiftGroup toTimingShiftGroup(ShiftTimingGroupDto shiftTimingGroup) {
        return new ShiftGroup(
                shiftTimingGroup.label(),
                List.of(),
                shiftTimingGroup.shifts().stream().map(this::toEmployeeScheduledShift).toList()
        );
    }

    private EmployeeScheduledShift toEmployeeScheduledShift(EmployeeScheduledShiftDto dto) {
        return new EmployeeScheduledShift(
                dto.shiftId(),
                dto.employeeId(),
                dto.firstName(),
                dto.lastName(),
                dto.phones(),
                formatTime(dto.startTime()),
                formatTime(dto.endTime()),
                dto.position(),
                dto.category(),
                dto.description(),
                dto.duration(),
                dto.color()
        );
    }

    private List<EmployeeShiftProjection> findEmployeeShiftRows(Integer companyId, LocalDate startDate, LocalDate endDate) {
        return schedulingQueryRepository.findAllEmployeeShiftsInRange(
                companyId,
                startDate.minusDays(1),
                endDate
        );
    }

    private List<EmployeeShiftProjection> findEmployeeShiftRows(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            Set<Integer> positionIds,
            Set<Integer> categoryIds
    ) {
        if (positionIds.isEmpty() && categoryIds.isEmpty()) {
            return findEmployeeShiftRows(companyId, startDate, endDate);
        }
        return schedulingQueryRepository.findAllEmployeeShiftsInRange(
                companyId,
                startDate.minusDays(1),
                endDate,
                new ArrayList<>(positionIds),
                new ArrayList<>(categoryIds)
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
                        startDate,
                        endDate
                )
        );
    }

    private void addShiftSegment(EmployeeSchedule employeeDto, LocalDate rangeStartDate, ShiftSegment segment) {
        int dayIndex = Math.toIntExact(ChronoUnit.DAYS.between(rangeStartDate, segment.date()));
        employeeDto.addShiftToDay(dayIndex, formatBucketDate(segment.date()), new ShiftSummary(
                segment.shiftId(),
                formatTime(segment.startTime()),
                formatTime(segment.endTime()),
                segment.position(),
                segment.categoryShortDescription(),
                segment.description(),
                segment.durationHours(),
                segment.color()
        ));
        employeeDto.setTotalHours(addHours(employeeDto.getTotalHours(), segment.durationHours()));
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
                row.getPositionId(),
                row.getPosition(),
                row.getCategoryId(),
                row.getCategory(),
                resolveCategoryShortDescription(row),
                row.getDescription(),
                durationHours,
                row.getSchedulePublished(),
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
                positions.add(new PositionShiftBucket(position, new ArrayList<>(), 0, scaledHours(0.0f)));
            }
            grouped.put(i, new DayPositionBucket(
                    formatBucketWeekday(bucketDate),
                    formatBucketDate(bucketDate),
                    positions,
                    0,
                    scaledHours(0.0f)
            ));
        }

        return grouped;
    }

    private Map<LocalDate, DayPositionTimingBucketDto> initializeDayPositionTimingBuckets(
            LocalDate startDate,
            LocalDate endDate,
            List<String> companyPositions
    ) {
        Map<LocalDate, DayPositionTimingBucketDto> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            List<PositionTimingBucketDto> positions = new ArrayList<>();
            for (String position : companyPositions) {
                positions.add(new PositionTimingBucketDto(position, new ArrayList<>(), 0, 0.0f));
            }
            grouped.put(bucketDate, new DayPositionTimingBucketDto(bucketDate, positions, 0, 0.0f));
        }

        return grouped;
    }

    private Map<LocalDate, DayCategoryTimingBucketDto> initializeDayCategoryTimingBuckets(
            LocalDate startDate,
            LocalDate endDate,
            List<String> companyCategories
    ) {
        Map<LocalDate, DayCategoryTimingBucketDto> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            List<CategoryTimingBucketDto> categories = new ArrayList<>();
            for (String category : companyCategories) {
                categories.add(new CategoryTimingBucketDto(category, new ArrayList<>()));
            }
            grouped.put(bucketDate, new DayCategoryTimingBucketDto(bucketDate, categories));
        }

        return grouped;
    }

    private Map<LocalDate, DayShiftTimingBucketDto> initializeDayShiftTimingBuckets(
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, DayShiftTimingBucketDto> grouped = new LinkedHashMap<>();
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return grouped;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            grouped.put(bucketDate, new DayShiftTimingBucketDto(bucketDate, new ArrayList<>()));
        }

        return grouped;
    }

    private List<String> getCompanyPositionNames(Integer companyId) {
        return getCompanyPositionNames(companyId, Set.of());
    }

    private List<String> getCompanyPositionNames(Integer companyId, Set<Integer> positionIds) {
        if (companyId == null || companyId == -1) {
            return List.of();
        }

        return positionService.get("all").stream()
                .filter(position -> positionIds.isEmpty() || positionIds.contains(position.positionId()))
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

        PositionShiftBucket positionBucket = new PositionShiftBucket(position, new ArrayList<>(), 0, scaledHours(0.0f));
        dayBucket.positions().add(positionBucket);
        return positionBucket;
    }

    private Map<String, String> buildCategoryLabelByName(Integer companyId, boolean useShortName, Set<Integer> categoryIds) {
        if (companyId == null || companyId == -1) {
            return Map.of();
        }

        Map<String, String> labels = new LinkedHashMap<>();
        for (CategorySummary category : categoryService.getCategoriesByCompanyId()) {
            if (category.description() == null) {
                continue;
            }
            if (!categoryIds.isEmpty() && !categoryIds.contains(category.id())) {
                continue;
            }
            labels.put(category.description(), useShortName ? preferredCategoryLabel(category) : category.description());
        }
        return labels;
    }

    private String preferredCategoryLabel(CategorySummary category) {
        if (category.shortDesc() != null && !category.shortDesc().isBlank()) {
            return category.shortDesc();
        }
        return category.description();
    }

    private String resolveCategoryLabel(Map<String, String> categoryLabelByName, String categoryName) {
        return categoryLabelByName.getOrDefault(categoryName, categoryName);
    }

    private PositionTimingBucketDto getOrCreatePositionTimingBucket(DayPositionTimingBucketDto dayBucket, String position) {
        for (PositionTimingBucketDto positionBucket : dayBucket.positions()) {
            if (Objects.equals(positionBucket.position(), position)) {
                return positionBucket;
            }
        }

        PositionTimingBucketDto positionBucket = new PositionTimingBucketDto(position, new ArrayList<>(), 0, 0.0f);
        dayBucket.positions().add(positionBucket);
        return positionBucket;
    }

    private CategoryTimingBucketDto getOrCreateCategoryTimingBucket(DayCategoryTimingBucketDto dayBucket, String category) {
        for (CategoryTimingBucketDto categoryBucket : dayBucket.categories()) {
            if (Objects.equals(categoryBucket.category(), category)) {
                return categoryBucket;
            }
        }

        CategoryTimingBucketDto categoryBucket = new CategoryTimingBucketDto(category, new ArrayList<>());
        dayBucket.categories().add(categoryBucket);
        return categoryBucket;
    }

    private ShiftTimingBucketDto getOrCreateShiftTimingBucket(
            PositionTimingBucketDto positionBucket,
            LocalTime startTime,
            LocalTime endTime
    ) {
        for (ShiftTimingBucketDto shiftTimingBucket : positionBucket.shiftTimings()) {
            if (Objects.equals(shiftTimingBucket.startTime(), startTime)
                    && Objects.equals(shiftTimingBucket.endTime(), endTime)) {
                return shiftTimingBucket;
            }
        }

        ShiftTimingBucketDto shiftTimingBucket = new ShiftTimingBucketDto(
                startTime,
                endTime,
                new ArrayList<>(),
                0,
                0.0f
        );
        positionBucket.shiftTimings().add(shiftTimingBucket);
        return shiftTimingBucket;
    }

    private ShiftTimingGroupDto getOrCreateShiftTimingGroup(
            CategoryTimingBucketDto categoryBucket,
            LocalTime startTime,
            LocalTime endTime
    ) {
        String label = formatShiftTimingLabel(startTime, endTime);
        for (ShiftTimingGroupDto shiftTimingBucket : categoryBucket.shiftTimings()) {
            if (Objects.equals(shiftTimingBucket.label(), label)) {
                return shiftTimingBucket;
            }
        }

        ShiftTimingGroupDto shiftTimingBucket = new ShiftTimingGroupDto(label, new ArrayList<>());
        categoryBucket.shiftTimings().add(shiftTimingBucket);
        return shiftTimingBucket;
    }

    private ShiftTimingGroupDto getOrCreateShiftTimingGroup(
            DayShiftTimingBucketDto dayBucket,
            LocalTime startTime,
            LocalTime endTime
    ) {
        String label = formatShiftTimingLabel(startTime, endTime);
        for (ShiftTimingGroupDto shiftTimingBucket : dayBucket.shiftTimings()) {
            if (Objects.equals(shiftTimingBucket.label(), label)) {
                return shiftTimingBucket;
            }
        }

        ShiftTimingGroupDto shiftTimingBucket = new ShiftTimingGroupDto(label, new ArrayList<>());
        dayBucket.shiftTimings().add(shiftTimingBucket);
        return shiftTimingBucket;
    }

    private void updateShiftTimingBucket(
            PositionTimingBucketDto positionBucket,
            ShiftTimingBucketDto shiftTimingBucket,
            float durationHours
    ) {
        int index = positionBucket.shiftTimings().indexOf(shiftTimingBucket);
        positionBucket.shiftTimings().set(
                index,
                new ShiftTimingBucketDto(
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
                        addHours(positionBucket.totalDuration(), durationHours)
                )
        );
    }

    private void updatePositionTimingBucket(
            DayPositionTimingBucketDto dayBucket,
            PositionTimingBucketDto positionBucket,
            float durationHours
    ) {
        int index = dayBucket.positions().indexOf(positionBucket);
        dayBucket.positions().set(
                index,
                new PositionTimingBucketDto(
                        positionBucket.position(),
                        positionBucket.shiftTimings(),
                        positionBucket.shiftCount() + 1,
                        positionBucket.totalDuration() + durationHours
                )
        );
    }

    private String formatShiftTimingLabel(LocalTime startTime, LocalTime endTime) {
        return formatTime(startTime) + "-" + formatTime(endTime);
    }

    private Set<Integer> toFilterSet(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(ids);
    }

    private boolean matchesFilters(ShiftSegment segment, Set<Integer> positionIds, Set<Integer> categoryIds) {
        boolean matchesPosition = positionIds.isEmpty() || positionIds.contains(segment.positionId());
        boolean matchesCategory = categoryIds.isEmpty() || categoryIds.contains(segment.categoryId());
        return matchesPosition && matchesCategory;
    }

    private String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        DateTimeFormatter formatter = time.getMinute() == 0 ? SHORT_SHIFT_TIME_FORMATTER : LONG_SHIFT_TIME_FORMATTER;
        return time.format(formatter).toLowerCase(Locale.ENGLISH);
    }

    private String formatBucketWeekday(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DAY_BUCKET_WEEKDAY_FORMATTER);
    }

    private String formatBucketDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.toString();
    }

    private String formatTitle(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            return null;
        }
        if (endDate != null && startDate.equals(endDate)) {
            return startDate.format(TITLE_SINGLE_DAY_FORMATTER);
        }
        return "Week of " + startDate.format(WEEK_TEXT_FORMATTER);
    }

    private BigDecimal scaledHours(float hours) {
        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal addHours(BigDecimal current, float hours) {
        BigDecimal base = current == null ? scaledHours(0.0f) : current;
        return base.add(scaledHours(hours)).setScale(2, RoundingMode.HALF_UP);
    }

    private void updatePublishedStage(EmployeeSchedule employeeDto, EmployeeShiftProjection row) {
        if (row.getSchedulePublished() == null || row.getShiftId() == null) {
            return;
        }

        String nextStatus = Boolean.TRUE.equals(row.getSchedulePublished()) ? "Published" : "Unpublished";
        if (employeeDto.getPublishedStage() == null || employeeDto.getPublishedStage().isBlank()) {
            employeeDto.setPublishedStage(nextStatus);
            return;
        }

        if (!employeeDto.getPublishedStage().equals(nextStatus)) {
            employeeDto.setPublishedStage("Mixed");
        }
    }

    private String resolveCategoryShortDescription(EmployeeShiftProjection row) {
        return row.getCategoryShortDescription();
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
            Integer positionId,
            String position,
            Integer categoryId,
            String category,
            String categoryShortDescription,
            String description,
            Float durationHours,
            Boolean schedulePublished,
            String color
    ) {
    }
}
