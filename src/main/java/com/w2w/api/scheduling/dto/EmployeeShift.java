package com.w2w.api.scheduling.dto;

import java.util.List;

public record EmployeeShift(
        Integer shiftId,
        Integer employeeId,
        String firstName,
        String lastName,
        List<String> phones,
        String employmentType,
        Integer empTypeId,
        String startTime,
        String endTime,
        String category,
        String description,
        Float duration,
        String color
) {
}
