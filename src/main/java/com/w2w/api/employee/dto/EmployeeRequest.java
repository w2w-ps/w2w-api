package com.w2w.api.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeRequest(
    @NotBlank String firstName,
    @NotBlank String lastName,
    @Email String email,
    String employeeNumber,
    String phone,
    String phone2,
    String cell,
    LocalDateTime hireDate,
    Integer maxScheduledHours,
    Integer maxDailyHours,
    Float payRate,
    Integer empTypeId,
    @Valid AddressRequest address,
    Integer maxWeeklyDays,
    Integer maxDailyShifts,
    String comments,
    String priorityGroup,
    Boolean googleCalExport,
    LocalDate nextAlertDate,
    String customField1,
    String customField2,
    String employeePhoto,
    List<Integer> positionIds,
    Boolean accessibilityMode
) {
    public record AddressRequest(
        String address,
        String address2,
        String city,
        String state,
        String zip
    ) {}
}
