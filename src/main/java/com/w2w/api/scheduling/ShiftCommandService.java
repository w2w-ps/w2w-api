package com.w2w.api.scheduling;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.ShiftResponse;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

@Service
public class ShiftCommandService {
    private final ShiftRepository shiftRepository;
    private final ScheduleRepository scheduleRepository;

    public ShiftCommandService(ShiftRepository shiftRepository, ScheduleRepository scheduleRepository) {
        this.shiftRepository = shiftRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public ShiftResponse getShift(Integer shiftId) {
        ShiftDetailsProjection shift = shiftRepository.findShiftDetailsByShiftIdAndCompanyId(
                        shiftId,
                        TenantContext.getCurrentTenant()
                )
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
        Shift shift = new Shift();
        shift.setEmployeeId(employeeId);
        shift.setCompanyId(CurrentTenant.requireCurrentTenant());
        shift.setDescription(description);
        shift.setStartTime(startTime);
        shift.setEndTime(endTime);
        shift.setRequiredPositionId(position);
        shift.setCategoryId(category);
        shift.setColor(color);
        shift.setIsDeleted(false);

        if (date != null) {
            Schedule schedule = getOrCreateSchedule(shift.getCompanyId(), date);
            shift.setScheduleId(schedule.getScheduleId());
        }

        applyDerivedShiftFields(shift, duration);
        shift.setChangedBy(shift.getEmployeeId());
        return shiftRepository.save(shift);
    }

    public ShiftResponse updateShift(Integer shiftId, UpdateShiftRequest request) {
        Shift shift = shiftRepository.findByShiftIdAndCompanyId(shiftId, CurrentTenant.requireCurrentTenant())
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
        Shift shift = shiftRepository.findByShiftIdAndCompanyId(shiftId, CurrentTenant.requireCurrentTenant())
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

    private float calculateDurationHours(LocalTime startTime, LocalTime endTime) {
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        if (minutes < 0) {
            minutes += 24 * 60;
        }
        return minutes / 60.0f;
    }
}
