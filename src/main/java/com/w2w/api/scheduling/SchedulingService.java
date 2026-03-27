package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.ShiftDto;
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

    public Shift saveShift(Shift shift) {
        return shiftRepository.save(shift);
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
