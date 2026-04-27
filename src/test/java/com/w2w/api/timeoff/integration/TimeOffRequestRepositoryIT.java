package com.w2w.api.timeoff.integration;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.tenant.Company;
import com.w2w.api.tenant.CompanyRepository;
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

    @Autowired
    private TimeOffRequestRepository timeOffRequestRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void findRequests_filtersByTenantEmployeeStatusAndDateRange() {
        Integer employeeAId = createCompanyAndEmployee(COMPANY_A_ID).getEmployeeId();
        Integer employeeBId = createCompanyAndEmployee(COMPANY_B_ID).getEmployeeId();

        createRequest(COMPANY_A_ID, employeeAId, LocalDate.of(2024, 11, 15), "PENDING", 1, 2, "Vacation Time");
        createRequest(COMPANY_A_ID, employeeAId, LocalDate.of(2023, 6, 5), "APPROVED", 1, 1, "Personal Day");
        createRequest(COMPANY_B_ID, employeeBId, LocalDate.of(2024, 11, 15), "PENDING", 1, 1, "Other tenant");

        TenantContext.setCurrentTenant(COMPANY_A_ID);

        List<TimeOffRequest> requests = timeOffRequestRepository.findRequests(
                COMPANY_A_ID,
                employeeAId,
                "PENDING",
                LocalDate.of(2024, 11, 1),
                LocalDate.of(2024, 11, 30)
        );

        assertEquals(1, requests.size());
        assertEquals(COMPANY_A_ID, requests.getFirst().getCompanyId());
        assertEquals(employeeAId, requests.getFirst().getEmployeeId());
        assertEquals(LocalTime.of(8, 0), requests.getFirst().getStartTime());
        assertEquals(LocalTime.of(23, 59), requests.getFirst().getEndTime());
        assertEquals(2, requests.getFirst().getRepeatCount());
    }

    private Employee createCompanyAndEmployee(int companyId) {
        TenantContext.setCurrentTenant(companyId);

        Company company = new Company();
        company.setCompanyId(companyId);
        company.setCompanyName("Time Off Company " + companyId);
        company.setDepartmentName("Operations");
        company.setStatus("active");
        companyRepository.save(company);

        Employee employee = new Employee();
        employee.setCompanyId(companyId);
        employee.setStatus("active");
        employee.setFirstName("Employee");
        employee.setLastName(String.valueOf(companyId));
        employee.setEmail("employee" + companyId + "@example.com");
        employee.setHireDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        return employeeRepository.save(employee);
    }

    private void createRequest(
            int companyId,
            int employeeId,
            LocalDate startDate,
            String status,
            int dayCount,
            int repeatCount,
            String comments
    ) {
        TenantContext.setCurrentTenant(companyId);
        TimeOffRequest request = new TimeOffRequest();
        request.setCompanyId(companyId);
        request.setEmployeeId(employeeId);
        request.setStartDate(startDate);
        request.setEndDate(startDate);
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(23, 59));
        request.setDayCount(dayCount);
        request.setRequestedAt(LocalDateTime.of(2024, 9, 27, 7, 17));
        request.setStatus(status);
        request.setComments(comments);
        request.setRepeatCount(repeatCount);
        timeOffRequestRepository.save(request);
    }
}
