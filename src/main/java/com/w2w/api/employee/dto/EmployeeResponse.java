package com.w2w.api.employee.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeResponse(
    Integer employeeId,
    Integer companyId,
    String status,
    LocalDateTime lastLogon,
    Integer logonCount,
    String firstName,
    String lastName,
    String username,
    String email,
    String employeeNumber,
    String phone,
    String phone2,
    String cell,
    LocalDateTime hireDate,
    Integer maxScheduledHours,
    Integer maxDailyHours,
    Float payRate,
    EmpTypeSummary empType,
    AddressSummary address,
    Integer maxWeeklyDays,
    Integer maxDailyShifts,
    String comments,
    String priorityGroup,
    Boolean googleCalExport,
    LocalDate nextAlertDate,
    String customField1,
    String customField2,
    String employeePhoto,
    Boolean accessibilityMode
) {
    public record AddressSummary(
        String address,
        String address2,
        String city,
        String state,
        String zip
    ) {}

    public record EmpTypeSummary(
        Integer id,
        String name
    ) {}
}
