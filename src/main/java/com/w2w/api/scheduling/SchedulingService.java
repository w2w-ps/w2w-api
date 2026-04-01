package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictDto;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.ShiftDto; // ShiftDto is now a class
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.OperationType;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap; // Import LinkedHashMap
import java.util.List;
import java.util.Map;

@Service
public class SchedulingService {
    @Autowired
    private SchedulingQueryRepository schedulingQueryRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private RuleEngineService ruleEngineService; // Inject the new service

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

    public Shift saveShift(com.w2w.api.scheduling.dto.CreateShiftRequest request) {
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
            // Assuming CreateShiftRequest has getCompanyId()
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.getDate());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.getDuration());

        // Assuming Shift model has getEmployeeId()
        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public Shift updateShift(Integer shiftId, com.w2w.api.scheduling.dto.UpdateShiftRequest request) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));

        // Using record accessors for UpdateShiftRequest
        if (request.employeeId() != null) shift.setEmployeeId(request.employeeId());
        if (request.description() != null) shift.setDescription(request.description());
        if (request.startTime() != null) shift.setStartTime(request.startTime());
        if (request.endTime() != null) shift.setEndTime(request.endTime());
        if (request.position() != null) shift.setRequiredSkillId(request.position());
        if (request.category() != null) shift.setCategoryId(request.category());
        if (request.color() != null) shift.setColor(request.color());

        if (request.date() != null) {
            // Assuming UpdateShiftRequest has getCompanyId() or it can be inferred/provided
            // For now, assuming shift object already has companyId
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.date());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.duration());

        // Assuming Shift model has getEmployeeId()
        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public void softDeleteShift(Integer shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + shiftId));
        shift.setIsDeleted(true);
        shiftRepository.save(shift);
    }

    private Schedule getOrCreateSchedule(Integer companyId, LocalDate date) {
        return scheduleRepository.findByCompanyIdAndStartDate(companyId, date)
                .orElseGet(() -> {
                    Schedule newSchedule = new Schedule();
                    newSchedule.setCompanyId(companyId);
                    newSchedule.setStartDate(date);
                    newSchedule.setPublished(false);
                    newSchedule.setDayOfWeek((short) date.getDayOfWeek().getValue());
                    newSchedule.setTimestamp(java.time.LocalDateTime.now());
                    return scheduleRepository.save(newSchedule);
                });
    }

    private void applyDerivedShiftFields(Shift shift, Float duration) {
        if (shift.getStartTime() != null && shift.getEndTime() != null) {
            if (duration != null) {
                shift.setDuration(duration);
            } else {
                shift.setDuration(calculateDurationHours(shift.getStartTime(), shift.getEndTime()));
            }
            shift.setIsOvernight(shift.getEndTime().isBefore(shift.getStartTime()));
        }
    }

    public List<EmployeeWithShiftsDto> getEmployeeShiftsGroupedInRange(Integer companyId, LocalDate startDate, LocalDate endDate) {
        List<EmployeeShiftProjection> flatResults = schedulingQueryRepository.findAllEmployeeShiftsInRange(
                companyId,
                startDate.minusDays(1),
                endDate
        );
        Map<Integer, EmployeeWithShiftsDto> grouped = new LinkedHashMap<>();

        for (EmployeeShiftProjection row : flatResults) {
            EmployeeWithShiftsDto employeeDto = getOrCreateEmployeeDto(grouped, row, startDate, endDate);

            if (!hasShiftData(row)) {
                continue;
            }

            boolean shiftAdded = false;
            LocalDate scheduleDate = row.getWeekCommencing();

            if (Boolean.TRUE.equals(row.getIsOvernight())) {
                if (isWithinRange(scheduleDate, startDate, endDate)) {
                    addShiftSegment(
                            employeeDto,
                            row,
                            startDate,
                            scheduleDate,
                            row.getStartTime(),
                            LocalTime.MIDNIGHT,
                            calculateDurationHours(row.getStartTime(), LocalTime.MIDNIGHT)
                    );
                    shiftAdded = true;
                }

                LocalDate nextDay = scheduleDate.plusDays(1);
                if (isWithinRange(nextDay, startDate, endDate)) {
                    addShiftSegment(
                            employeeDto,
                            row,
                            startDate,
                            nextDay,
                            LocalTime.MIDNIGHT,
                            row.getEndTime(),
                            calculateDurationHours(LocalTime.MIDNIGHT, row.getEndTime())
                    );
                    shiftAdded = true;
                }
            } else if (isWithinRange(scheduleDate, startDate, endDate)) {
                addShiftSegment(
                        employeeDto,
                        row,
                        startDate,
                        scheduleDate,
                        row.getStartTime(),
                        row.getEndTime(),
                        resolveDuration(row)
                );
                shiftAdded = true;
            }

            if (shiftAdded) {
                employeeDto.setShiftCount(employeeDto.getShiftCount() + 1);
            }
        }

        return new ArrayList<>(grouped.values());
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

    private void addShiftSegment(
            EmployeeWithShiftsDto employeeDto,
            EmployeeShiftProjection row,
            LocalDate rangeStartDate,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            float durationHours
    ) {
        int dayIndex = Math.toIntExact(ChronoUnit.DAYS.between(rangeStartDate, date));
        // Correcting ShiftDto constructor call to match its class structure and arguments
        // Assuming EmployeeShiftProjection has getCompanyId()
        employeeDto.addShiftToDay(dayIndex, date, new ShiftDto(
                row.getShiftId(),
                startTime,
                endTime,
                row.getPosition(),
                row.getCategory(),
                row.getDescription(),
                durationHours,
                row.getColor()
        ));
        employeeDto.setTotalHours(employeeDto.getTotalHours() + durationHours);
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

    // Updated validate method to use FindConflictRequest's new structure
    // and rely on essential parameter checks and RuleEngineService delegation.
    public List<ConflictDto> validate(FindConflictRequest request) {
        // Removed all explicit parameter checks (for null operationType, shiftData, shiftId, newEmployeeId).
        // The method now solely delegates to the RuleEngineService.
        // The RuleEngineService methods currently return empty lists due to TODOs.

        // Delegate to RuleEngineService for all rule checks
        List<ConflictDto> conflicts = new ArrayList<>();
        conflicts.addAll(ruleEngineService.checkMaxHoursPerDay(request));
        conflicts.addAll(ruleEngineService.checkMaxShiftsPerDay(request));
        conflicts.addAll(ruleEngineService.checkMaxHoursAndShiftsPerWeek(request));
        conflicts.addAll(ruleEngineService.checkWorkPreferences(request));
        conflicts.addAll(ruleEngineService.checkTimeOff(request));
        conflicts.addAll(ruleEngineService.checkConflictWithExistingShifts(request));

        return conflicts;
    }
}
