package com.w2w.api.timeoff.integration;

import com.w2w.api.config.TenantContext;
import com.w2w.api.timeoff.TimeOffRequest;
import com.w2w.api.timeoff.TimeOffRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class TimeOffRequestRepositoryIT extends com.w2w.api.scheduling.integration.PostgresIntegrationTestBase {

    private static final int COMPANY_A_ID = 7041;
    private static final int COMPANY_B_ID = 7042;
    private static final int EMPLOYEE_A_ID = 704101;
    private static final int EMPLOYEE_B_ID = 704201;

    @Autowired
    private TimeOffRequestRepository timeOffRequestRepository;

    @Test
    void findRequests_filtersByTenantEmployeeStatusAndDateRange() {
        createCompanyEmployeeAndRequest(COMPANY_A_ID, EMPLOYEE_A_ID, LocalDate.of(2024, 11, 15), "PENDING", 1, 2, "Vacation Time");
        createCompanyEmployeeAndRequest(COMPANY_A_ID, EMPLOYEE_A_ID, LocalDate.of(2023, 6, 5), "APPROVED", 1, 1, "Personal Day");
        createCompanyEmployeeAndRequest(COMPANY_B_ID, EMPLOYEE_B_ID, LocalDate.of(2024, 11, 15), "PENDING", 1, 1, "Other tenant");

        TenantContext.setCurrentTenant(COMPANY_A_ID);

        List<TimeOffRequest> requests = timeOffRequestRepository.findRequests(
                COMPANY_A_ID,
                EMPLOYEE_A_ID,
                "PENDING",
                LocalDate.of(2024, 11, 1),
                LocalDate.of(2024, 11, 30)
        );

        assertEquals(1, requests.size());
        assertEquals(COMPANY_A_ID, requests.getFirst().getCompanyId());
        assertEquals(EMPLOYEE_A_ID, requests.getFirst().getEmployeeId());
        assertEquals(LocalTime.of(8, 0), requests.getFirst().getStartTime());
        assertEquals(LocalTime.of(23, 59), requests.getFirst().getEndTime());
        assertEquals(2, requests.getFirst().getRepeatCount());
    }

    private void createCompanyEmployeeAndRequest(
            int companyId,
            int employeeId,
            LocalDate startDate,
            String status,
            int dayCount,
            int repeatCount,
            String comments
    ) {
        TenantContext.setCurrentTenant(companyId);
        jdbcTemplate.update(
                "INSERT INTO company (company_id, company_name, department_name, status) VALUES (?, ?, ?, ?)",
                companyId,
                "Time Off Company " + companyId,
                "Operations",
                "active"
        );
        TenantContext.setCurrentTenant(companyId);
        jdbcTemplate.update(
                "INSERT INTO employee (employee_id, company_id, status, first_name, last_name, email, hire_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                employeeId,
                companyId,
                "active",
                "Employee",
                String.valueOf(companyId),
                "employee" + companyId + "@example.com",
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        TenantContext.setCurrentTenant(companyId);
        jdbcTemplate.update(
                """
                INSERT INTO time_off_request (
                    company_id,
                    employee_id,
                    start_date,
                    end_date,
                    start_time,
                    end_time,
                    day_count,
                    requested_at,
                    status,
                    comments,
                    repeat_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                companyId,
                employeeId,
                startDate,
                startDate,
                LocalTime.of(8, 0),
                LocalTime.of(23, 59),
                dayCount,
                LocalDateTime.of(2024, 9, 27, 7, 17),
                status,
                comments,
                repeatCount
        );
    }
}