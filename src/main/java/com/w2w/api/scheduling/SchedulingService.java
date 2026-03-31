package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.ShiftDto;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    public ShiftResponseDto getShift(Integer transactionId) {
        ShiftDetailsProjection shift = shiftRepository.findShiftDetailsByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + transactionId));

        ShiftResponseDto response = new ShiftResponseDto();
        response.setTransactionId(shift.getTransactionId());
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
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.getDate());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.getDuration());

        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public Shift updateShift(Integer transactionId, com.w2w.api.scheduling.dto.UpdateShiftRequest request) {
        Shift shift = shiftRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + transactionId));

        if (request.getEmployeeId() != null) shift.setEmployeeId(request.getEmployeeId());
        if (request.getDescription() != null) shift.setDescription(request.getDescription());
        if (request.getStartTime() != null) shift.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) shift.setEndTime(request.getEndTime());
        if (request.getPosition() != null) shift.setRequiredSkillId(request.getPosition());
        if (request.getCategory() != null) shift.setCategoryId(request.getCategory());
        if (request.getColor() != null) shift.setColor(request.getColor());

        if (request.getDate() != null) {
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), request.getDate());
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, request.getDuration());

        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public void softDeleteShift(Integer transactionId) {
        Shift shift = shiftRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with id: " + transactionId));
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
        employeeDto.addShiftToDay(dayIndex, date, new ShiftDto(
                startTime,
                endTime,
                row.getPosition(),
                row.getCategory(),
                row.getDescription(),
                durationHours
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
}
