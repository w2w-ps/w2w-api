package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SchedulingShiftLookupService {
    private final ShiftRepository shiftRepository;

    public SchedulingShiftLookupService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    public List<DailyHoursShiftProjection> findActiveDailyHoursShifts(
            Integer companyId,
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate,
            Integer excludedShiftId
    ) {
        return shiftRepository.findActiveDailyHoursShiftsByCompanyIdAndEmployeeIdAndDateRangeExcludingShiftId(
                companyId,
                employeeId,
                startDate,
                endDate,
                excludedShiftId
        );
    }

    public List<Shift> findActiveSameDayShifts(
            Integer companyId,
            Integer employeeId,
            LocalDate date,
            Integer excludedShiftId
    ) {
        return shiftRepository.findActiveByCompanyIdAndEmployeeIdAndDateExcludingShiftId(
                companyId,
                employeeId,
                date,
                excludedShiftId
        );
    }
}
