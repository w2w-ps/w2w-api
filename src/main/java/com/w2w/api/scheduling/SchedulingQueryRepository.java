package com.w2w.api.scheduling;


import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import java.time.LocalDate;
import java.util.List;

public interface SchedulingQueryRepository {
    List<EmployeeShiftProjection> findAllEmployeeShiftsInRange(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<EmployeeShiftProjection> findAllEmployeeShiftsInRange(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds
    );

    List<EmployeeShiftProjection> findAllEmployeeShiftsInRange(
            Integer companyId,
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> positionIds,
            List<Integer> categoryIds,
            Integer status
    );
}
