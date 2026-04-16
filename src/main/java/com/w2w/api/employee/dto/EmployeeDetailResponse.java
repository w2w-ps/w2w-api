package com.w2w.api.employee.dto;

import com.w2w.api.position.dto.PositionSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full detail view of an employee, including assigned positions.
 * Returned by GET /api/employees/{id}/view.
 */
public record EmployeeDetailResponse(
        Integer employeeId,
        Integer companyId,
        String status,
        LocalDateTime lastLogon,
        Integer logonCount,
        String firstName,
        String lastName,
        String email,
        String employeeNumber,
        List<String> phones,
        LocalDateTime hireDate,
        BigDecimal payRate,
        EmpTypeSummary empType,
        AddressSummary address,
        LocalDate nextAlertDate,
        String customField1,
        String customField2,
        String employeePhoto,
        // Positions section
        List<PositionSummary> positions,
        // AutoFill Options section
        Integer maxScheduledHours,
        Integer maxDailyHours,
        Integer maxWeeklyDays,
        Integer maxDailyShifts,
        String priorityGroup,
        // Comment section
        String comments,
        Boolean googleCalExport) {
    public record AddressSummary(
            String address,
            String address2,
            String city,
            String state,
            String zip) {
    }

    public record EmpTypeSummary(
            Integer id,
            String name) {
    }
}
