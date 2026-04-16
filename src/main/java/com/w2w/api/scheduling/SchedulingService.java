package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.*;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.SecurityUtils;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.notification.NotificationProducer;
import com.w2w.api.notification.NotificationRequest;
import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.SchedulePartialPub;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class SchedulingService {
    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SchedulePartialPubRepository partialPubRepository;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private NotificationProducer notificationProducer;

    @Autowired
    private PositionService positionService;

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
        return shiftCommandService.saveShift(new CreateShiftCommand(
                employeeId,
                description,
                date,
                startTime,
                endTime,
                duration,
                position,
                category,
                color
        ));
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

    @Transactional
    public void publishSchedule(Integer scheduleId) {
        Integer companyId = CurrentTenant.requireCurrentTenant();
        Schedule schedule = scheduleRepository.findByScheduleIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found with id: " + scheduleId));

        schedule.setPublished(true);
        schedule.setLastChange(LocalDateTime.now());
        scheduleRepository.save(schedule);

        // If fully published, partial records are no longer needed
        partialPubRepository.deleteByScheduleId(scheduleId);

        // Notify all employees
        notificationProducer.sendNotification(new NotificationRequest("publish", null, scheduleId, null, companyId));
    }

    @Transactional
    public void unpublishSchedule(Integer scheduleId) {
        Integer companyId = CurrentTenant.requireCurrentTenant();
        Schedule schedule = scheduleRepository.findByScheduleIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found with id: " + scheduleId));

        schedule.setPublished(false);
        schedule.setLastChange(LocalDateTime.now());
        scheduleRepository.save(schedule);

        // Clear partial records on unpublish
        partialPubRepository.deleteByScheduleId(scheduleId);

        // Notify all employees
        notificationProducer.sendNotification(new NotificationRequest("unpublish", null, scheduleId, null, companyId));
    }

    @Transactional
    public void partialPublishSchedule(Integer scheduleId, List<Integer> positionIds) {
        Integer companyId = CurrentTenant.requireCurrentTenant();
        Schedule schedule = scheduleRepository.findByScheduleIdAndCompanyId(scheduleId, companyId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found with id: " + scheduleId));

        // Get current employee ID for audit
        String username = SecurityUtils.getCurrentUsername();
        Integer employeeId = null;
        if (username != null) {
            employeeId = loginRepository.findByLoginId(username)
                    .map(User::getEmployeeId)
                    .orElse(null);
        }

        // Add new partial publication records (cumulative)
        for (Integer positionId : positionIds) {
            partialPubRepository.save(new SchedulePartialPub(scheduleId, positionId, employeeId));
        }

        // Check if all active positions are now published
        List<PositionSummary> activePositions = positionService.get("active");
        List<Integer> publishedPositionIds = partialPubRepository.findByScheduleId(scheduleId).stream()
                .map(SchedulePartialPub::getRequiredPositionId)
                .toList();

        boolean allPublished = activePositions.stream()
                .allMatch(p -> publishedPositionIds.contains(p.positionId()));

        if (allPublished) {
            publishSchedule(scheduleId);
        } else {
            schedule.setPublished(false);
            schedule.setLastChange(LocalDateTime.now());
            scheduleRepository.save(schedule);

            notificationProducer
                    .sendNotification(new NotificationRequest("publish", null, scheduleId, positionIds, companyId));
        }
    }
}
