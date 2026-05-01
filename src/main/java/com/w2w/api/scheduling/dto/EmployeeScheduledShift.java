package com.w2w.api.scheduling.dto;

import java.util.List;

public record EmployeeScheduledShift(
        Integer shiftId,
        Integer employeeId,
        String firstName,
        String lastName,
        List<String> phones,
        Integer empTypeId,
        String startTime,
        String endTime,
        String position,
        String category,
        String description,
        Float duration,
        String color
) {
}
