package com.w2w.api.employee.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeRequest(
    String firstName,
    String lastName,
    String email,
    String employeeNumber,
    List<String> phones,
    LocalDateTime hireDate,
    Integer maxScheduledHours,
    Integer maxDailyHours,
    Float payRate,
    Integer empTypeId,
    AddressRequest address,
    Integer maxWeeklyDays,
    Integer maxDailyShifts,
    String comments,
    String priorityGroup,
    Boolean googleCalExport,
    LocalDate nextAlertDate,
    String customField1,
    String customField2,
    String employeePhoto
) {
    public record AddressRequest(
        String address,
        String address2,
        String city,
        String state,
        String zip
    ) {}
}
