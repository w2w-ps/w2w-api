package com.w2w.api.scheduling.integration;

import com.w2w.api.config.TenantContext;
import com.w2w.api.scheduling.ScheduleRepository;
import com.w2w.api.scheduling.ShiftRepository;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.tenant.Company;
import com.w2w.api.tenant.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SchedulingGroupedApiIT extends PostgresIntegrationTestBase {

    private static final Integer COMPANY_ID = 7021;
    private static final Integer EMPLOYEE_A_ID = 702101;
    private static final Integer EMPLOYEE_B_ID = 702102;
    private static final LocalDate SHIFT_DATE = LocalDate.of(2026, 5, 9);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    void groupedShiftsAppliesPositionAndCategoryFiltersThroughApi() throws Exception {
        createCompany();
        TenantContext.setCurrentTenant(COMPANY_ID);
        createEmployee(EMPLOYEE_A_ID, "Ava", "Stone");
        createEmployee(EMPLOYEE_B_ID, "Ben", "Miles");
        Integer bartenderId = createPosition("Bartender");
        Integer serverId = createPosition("Server");
        Integer frontCategoryId = createCategory("Front", "FRT");
        Integer floorCategoryId = createCategory("Floor", "FLR");
        Schedule schedule = createSchedule();
        createShift(EMPLOYEE_A_ID, schedule.getScheduleId(), bartenderId, frontCategoryId, "amber");
        createShift(EMPLOYEE_B_ID, schedule.getScheduleId(), serverId, floorCategoryId, "blue");

        mockMvc.perform(get("/api/scheduling/shifts/grouped")
                        .param("companyId", COMPANY_ID.toString())
                        .param("grouping", "position_shift_timings")
                        .param("positionIds", bartenderId.toString())
                        .param("categoryIds", frontCategoryId.toString())
                        .param("startDate", SHIFT_DATE.toString())
                        .param("endDate", SHIFT_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].label").value("Bartender"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].label").value("9am-5pm"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts[0].firstName").value("Ava"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts[0].startTime").value("9am"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts[0].endTime").value("5pm"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts[0].category").value("FRT"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts[0].color").value("amber"));
    }

    private void createCompany() {
        TenantContext.setCurrentTenant(COMPANY_ID);
        Company company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Grouped API Tenant");
        company.setDepartmentName("Integration");
        company.setStatus("active");
        company.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        companyRepository.save(company);
    }

    private void createEmployee(Integer employeeId, String firstName, String lastName) {
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
                )
                VALUES (?, ?, 'active', ?, ?, ?, ?)
                """,
                employeeId,
                COMPANY_ID,
                firstName,
                lastName,
                firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com",
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    private Integer createPosition(String description) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO position (
                    company_id,
                    description,
                    is_deleted,
                    timestamp
                )
                VALUES (?, ?, false, ?)
                RETURNING position_id
                """,
                Integer.class,
                COMPANY_ID,
                description,
                LocalDateTime.of(2026, 5, 1, 0, 0)
        );
    }

    private Integer createCategory(String description, String shortDescription) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO category (
                    company_id,
                    short_desc,
                    description,
                    is_deleted
                )
                VALUES (?, ?, ?, false)
                RETURNING category_id
                """,
                Integer.class,
                COMPANY_ID,
                shortDescription,
                description
        );
    }

    private Schedule createSchedule() {
        Schedule schedule = new Schedule();
        schedule.setCompanyId(COMPANY_ID);
        schedule.setDescription("Grouped API schedule");
        schedule.setPublished(true);
        schedule.setStartDate(SHIFT_DATE);
        schedule.setDayOfWeek((short) DayOfWeek.from(SHIFT_DATE).getValue());
        schedule.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        return scheduleRepository.save(schedule);
    }

    private void createShift(
            Integer employeeId,
            Integer scheduleId,
            Integer positionId,
            Integer categoryId,
            String color
    ) {
        Shift shift = new Shift();
        shift.setEmployeeId(employeeId);
        shift.setCompanyId(COMPANY_ID);
        shift.setScheduleId(scheduleId);
        shift.setDescription("API shift");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(17, 0));
        shift.setDuration(8.0f);
        shift.setIsOvernight(false);
        shift.setRequiredPositionId(positionId);
        shift.setCategoryId(categoryId);
        shift.setColor(color);
        shift.setIsDeleted(false);
        shift.setChangedBy(employeeId);
        shiftRepository.save(shift);
    }
}
