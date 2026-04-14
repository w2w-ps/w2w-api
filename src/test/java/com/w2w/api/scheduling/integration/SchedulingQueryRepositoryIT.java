package com.w2w.api.scheduling.integration;

import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
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
    private static final Integer EMPLOYEE_A_ID = 701101;
    private static final Integer EMPLOYEE_B_ID = 701201;
    private static final LocalDate SHIFT_DATE = LocalDate.of(2026, 5, 9);

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

    @Test
    void findAllEmployeeShiftsInRange_mapsPhonesAvailablePositionsAndFiltersByTenant() {
        createCompany(COMPANY_A_ID, "Query Tenant A");
        createCompany(COMPANY_B_ID, "Query Tenant B");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of("111-222", "333-444"));
        Position bartender = createPosition(COMPANY_A_ID, "Bartender");
        Position server = createPosition(COMPANY_A_ID, "Server");
        assignEmployeeSkill(EMPLOYEE_A_ID, bartender.getPositionId());
        assignEmployeeSkill(EMPLOYEE_A_ID, server.getPositionId());
        Schedule companyASchedule = createSchedule(COMPANY_A_ID, SHIFT_DATE);
        createShift(EMPLOYEE_A_ID, COMPANY_A_ID, companyASchedule.getScheduleId(), bartender.getPositionId(), "amber");

        TenantContext.setCurrentTenant(COMPANY_B_ID);
        createEmployee(EMPLOYEE_B_ID, COMPANY_B_ID, "Ben", "Miles", List.of("999-000"));
        Position houseman = createPosition(COMPANY_B_ID, "Houseman");
        assignEmployeeSkill(EMPLOYEE_B_ID, houseman.getPositionId());
        Schedule companyBSchedule = createSchedule(COMPANY_B_ID, SHIFT_DATE);
        createShift(EMPLOYEE_B_ID, COMPANY_B_ID, companyBSchedule.getScheduleId(), houseman.getPositionId(), "green");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        List<EmployeeShiftProjection> rows = schedulingQueryRepository.findAllEmployeeShiftsInRange(
                COMPANY_A_ID,
                SHIFT_DATE,
                SHIFT_DATE
        );

        assertEquals(1, rows.size());
        EmployeeShiftProjection row = rows.getFirst();
        assertEquals(EMPLOYEE_A_ID, row.getEmployeeId());
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
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of("111-222"));
        Position bartender = createPosition(COMPANY_A_ID, "Bartender");
        assignEmployeeSkill(EMPLOYEE_A_ID, bartender.getPositionId());
        Schedule companyASchedule = createSchedule(COMPANY_A_ID, SHIFT_DATE);
        createShift(EMPLOYEE_A_ID, COMPANY_A_ID, companyASchedule.getScheduleId(), bartender.getPositionId(), "amber");
        createUserLogin("employee.701101", COMPANY_A_ID, EMPLOYEE_A_ID, "Employee");
        createUserLogin("manager.701101", COMPANY_A_ID, EMPLOYEE_A_ID, "Manager");

        List<EmployeeShiftProjection> rows = schedulingQueryRepository.findAllEmployeeShiftsInRange(
                COMPANY_A_ID,
                SHIFT_DATE,
                SHIFT_DATE
        );

        assertEquals(1, rows.size());
        assertEquals(EMPLOYEE_A_ID, rows.getFirst().getEmployeeId());
        assertEquals(null, rows.getFirst().getEmploymentType());
    }

    private void assignEmployeeSkill(Integer employeeId, Integer skillId) {
        jdbcTemplate.update(
                "INSERT INTO employee_position (employee_id, position_id) VALUES (?, ?)",
                employeeId,
                skillId
        );
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

    private void createUserLogin(String loginId, Integer companyId, Integer employeeId, String roleName) {
        jdbcTemplate.update(
                """
                INSERT INTO users (
                    user_login_id,
                    user_login_pw,
                    company_id,
                    role_id,
                    employee_id,
                    encryption_type,
                    login_failures
                )
                VALUES (
                    ?,
                    ?,
                    ?,
                    (SELECT role_id FROM user_roles WHERE role_name = ?),
                    ?,
                    0,
                    0
                )
                """,
                loginId,
                "$2a$12$9B69QSXuEqf6bgZcWbJXMOc0RHlFkwHQ4iInRtrIwiC9nAJSTgdk.",
                companyId,
                roleName,
                employeeId
        );
    }

    private void createEmployee(Integer employeeId, Integer companyId, String firstName, String lastName, List<String> phones) {
        jdbcTemplate.update(
                """
                INSERT INTO employee (
                    employee_id,
                    company_id,
                    status,
                    first_name,
                    last_name,
                    email,
                    hire_date
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                employeeId,
                companyId,
                "active",
                firstName,
                lastName,
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com",
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );

        for (int i = 0; i < phones.size(); i++) {
            jdbcTemplate.update(
                    "INSERT INTO employee_phone (employee_id, sort_order, phone_number) VALUES (?, ?, ?)",
                    employeeId,
                    i,
                    phones.get(i)
            );
        }
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
