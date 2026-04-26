package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.*;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class SchedulingService {
    private final ShiftCommandService shiftCommandService;
    private final SchedulingGroupingService schedulingGroupingService;
    private final RuleEngineService ruleEngineService;

    public SchedulingService(
            ShiftCommandService shiftCommandService,
            SchedulingGroupingService schedulingGroupingService,
            RuleEngineService ruleEngineService
    ) {
        this.shiftCommandService = shiftCommandService;
        this.schedulingGroupingService = schedulingGroupingService;
        this.ruleEngineService = ruleEngineService;
    }

    public ShiftResponse getShift(Integer shiftId) {
        return shiftCommandService.getShift(shiftId);
    }

    public Shift saveShift(
            Integer employeeId,
            String description,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Float duration,
            Integer position,
            Integer category,
            String color
    ) {
        return shiftCommandService.saveShift(
                employeeId,
                description,
                date,
                startTime,
                endTime,
                duration,
                position,
                category,
                color
        );
    }

    public ShiftResponse updateShift(Integer shiftId, UpdateShiftRequest request) {
        return shiftCommandService.updateShift(shiftId, request);
    }

    public void softDeleteShift(Integer shiftId) {
        shiftCommandService.softDeleteShift(shiftId);
    }

    public List<EmployeeSchedule> getEmployeeShiftsGroupedInRange(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        return schedulingGroupingService.getEmployeeShiftsGroupedInRange(startDate, endDate, positionIds, categoryIds);
    }

    public GroupedShiftsResponse getShiftsGrouped(
            LocalDate startDate,
            LocalDate endDate,
            ShiftGrouping grouping
    ) {
        return schedulingGroupingService.getShiftsGrouped(startDate, endDate, grouping);
    }

    public DatePositionSummaryResponse getShiftsGroupedByDateAndPosition(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    ) {
        return schedulingGroupingService.getShiftsGroupedByDateAndPosition(startDate, endDate, positionIds, categoryIds);
    }

    public List<DayPositionTimingBucketDto> getShiftsGroupedByDayPositionAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return schedulingGroupingService.getShiftsGroupedByDayPositionAndTiming(startDate, endDate);
    }

    public List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return schedulingGroupingService.getShiftsGroupedByDayCategoryAndTiming(startDate, endDate);
    }

    public List<DayCategoryTimingBucketDto> getShiftsGroupedByDayCategoryShortNameAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return schedulingGroupingService.getShiftsGroupedByDayCategoryShortNameAndTiming(startDate, endDate);
    }

    public List<DayShiftTimingBucketDto> getShiftsGroupedByDayAndTiming(
            LocalDate startDate,
            LocalDate endDate
    ) {
        return schedulingGroupingService.getShiftsGroupedByDayAndTiming(startDate, endDate);
    }

    public List<ConflictItem> validate(FindConflictRequest request) {
        return ruleEngineService.validate(request);
    }
}
