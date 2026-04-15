package com.w2w.api.scheduling.integration;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import com.w2w.api.login.EmpTypeRepository;
import com.w2w.api.login.LoginRepository;
import com.w2w.api.login.User;
import com.w2w.api.login.UserRoleRepository;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.scheduling.ScheduleRepository;
import com.w2w.api.scheduling.SchedulingQueryRepository;
import com.w2w.api.scheduling.ShiftRepository;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.tenant.Company;
import com.w2w.api.tenant.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SchedulingQueryRepositoryIT extends PostgresIntegrationTestBase {

    private static final Integer COMPANY_A_ID = 7011;
    private static final Integer COMPANY_B_ID = 7012;
    private static final LocalDate SHIFT_DATE = LocalDate.of(2026, 5, 9);
    private static final String DEFAULT_PASSWORD_HASH = "$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.";

    @Autowired
    private SchedulingQueryRepository schedulingQueryRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private EmpTypeRepository empTypeRepository;

    @Test
    void findAllEmployeeShiftsInRange_mapsPhonesAvailablePositionsAndFiltersByTenant() {
        createCompany(COMPANY_A_ID, "Query Tenant A");
        createCompany(COMPANY_B_ID, "Query Tenant B");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        Employee employeeA = createEmployee(COMPANY_A_ID, "Ava", "Stone", List.of("111-222", "333-444"));
        Position bartender = createPosition(COMPANY_A_ID, "Bartender");
        Position server = createPosition(COMPANY_A_ID, "Server");
        Schedule companyASchedule = createSchedule(COMPANY_A_ID, SHIFT_DATE);
        createShift(employeeA.getEmployeeId(), COMPANY_A_ID, companyASchedule.getScheduleId(), bartender.getPositionId(), "amber");

        TenantContext.setCurrentTenant(COMPANY_B_ID);
        Employee employeeB = createEmployee(COMPANY_B_ID, "Ben", "Miles", List.of("999-000"));
        Position houseman = createPosition(COMPANY_B_ID, "Houseman");
        Schedule companyBSchedule = createSchedule(COMPANY_B_ID, SHIFT_DATE);
        createShift(employeeB.getEmployeeId(), COMPANY_B_ID, companyBSchedule.getScheduleId(), houseman.getPositionId(), "green");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        List<EmployeeShiftProjection> rows = schedulingQueryRepository.findAllEmployeeShiftsInRange(
                COMPANY_A_ID,
                SHIFT_DATE,
                SHIFT_DATE
        );

        assertEquals(1, rows.size());
        EmployeeShiftProjection row = rows.getFirst();
        assertEquals(employeeA.getEmployeeId(), row.getEmployeeId());
        assertEquals(List.of("111-222", "333-444"), row.getPhones());
        assertEquals(2, row.getAvailablePositions().size());
        assertEquals("Bartender", row.getAvailablePositions().getFirst().description());
        assertEquals("Server", row.getAvailablePositions().get(1).description());
        assertEquals(LocalDate.of(2026, 5, 9), row.getWeekCommencing());
        assertEquals(LocalTime.of(9, 0), row.getStartTime());
        assertEquals(LocalTime.of(17, 0), row.getEndTime());
        assertEquals("Bartender", row.getPosition());
        assertEquals("Opening shift", row.getDescription());
        assertEquals("amber", row.getColor());
        assertEquals(null, row.getEmploymentType());
    }

    @Test
    void findAllEmployeeShiftsInRange_doesNotDuplicateRowsWhenEmployeeHasMultipleUsers() {
        createCompany(COMPANY_A_ID, "Query Tenant A");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        Employee employee = createEmployee(COMPANY_A_ID, "Ava", "Stone", List.of("111-222"));
        Position bartender = createPosition(COMPANY_A_ID, "Bartender");
        Schedule companyASchedule = createSchedule(COMPANY_A_ID, SHIFT_DATE);
        createShift(employee.getEmployeeId(), COMPANY_A_ID, companyASchedule.getScheduleId(), bartender.getPositionId(), "amber");
        createUserLogin("employee." + employee.getEmployeeId(), COMPANY_A_ID, employee, "Employee");
        createUserLogin("manager." + employee.getEmployeeId(), COMPANY_A_ID, employee, "Manager");

        List<EmployeeShiftProjection> rows = schedulingQueryRepository.findAllEmployeeShiftsInRange(
                COMPANY_A_ID,
                SHIFT_DATE,
                SHIFT_DATE
        );

        assertEquals(1, rows.size());
        assertEquals(employee.getEmployeeId(), rows.getFirst().getEmployeeId());
        assertEquals(null, rows.getFirst().getEmploymentType());
    }

    private void createCompany(Integer companyId, String companyName) {
        TenantContext.setCurrentTenant(companyId);
        Company company = new Company();
        company.setCompanyId(companyId);
        company.setCompanyName(companyName);
        company.setDepartmentName("Integration");
        company.setStatus("active");
        company.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        companyRepository.save(company);
    }

    private void createUserLogin(String loginId, Integer companyId, Employee employee, String roleName) {
        User user = new User();
        user.setLoginId(loginId);
        user.setPassword(DEFAULT_PASSWORD_HASH);
        user.setCompanyId(companyId);
        user.setEmployee(employee);
        user.setEmpType(empTypeRepository.findByName("Full Time").orElseThrow());
        user.setRole(userRoleRepository.findByName(roleName).orElseThrow());
        user.setEncryptionType(0);
        user.setLoginFailures(0);
        loginRepository.save(user);
    }

    private Employee createEmployee(Integer companyId, String firstName, String lastName, List<String> phones) {
        Employee employee = new Employee();
        employee.setCompanyId(companyId);
        employee.setStatus("active");
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        employee.setHireDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        employee.setPhones(phones);
        return employeeRepository.save(employee);
    }

    private Position createPosition(Integer companyId, String description) {
        Position position = new Position();
        position.setCompanyId(companyId);
        position.setDescription(description);
        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        return positionRepository.save(position);
    }

    private Schedule createSchedule(Integer companyId, LocalDate startDate) {
        Schedule schedule = new Schedule();
        schedule.setCompanyId(companyId);
        schedule.setDescription("Existing schedule");
        schedule.setPublished(true);
        schedule.setStartDate(startDate);
        schedule.setDayOfWeek((short) DayOfWeek.from(startDate).getValue());
        schedule.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        return scheduleRepository.save(schedule);
    }

    private void createShift(
            Integer employeeId,
            Integer companyId,
            Integer scheduleId,
            Integer positionId,
            String color
    ) {
        Shift shift = new Shift();
        shift.setEmployeeId(employeeId);
        shift.setCompanyId(companyId);
        shift.setScheduleId(scheduleId);
        shift.setDescription("Opening shift");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));
        shift.setDuration(8.0f);
        shift.setIsOvernight(false);
        shift.setRequiredPositionId(positionId);
        shift.setColor(color);
        shift.setIsDeleted(false);
        shift.setChangedBy(employeeId);
        shiftRepository.save(shift);
    }
}
