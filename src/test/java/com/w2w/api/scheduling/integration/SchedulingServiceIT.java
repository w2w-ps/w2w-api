package com.w2w.api.scheduling.integration;

import com.w2w.api.category.model.Category;
import com.w2w.api.category.repository.CategoryRepository;
import com.w2w.api.config.TenantContext;
import com.w2w.api.employee.Employee;
import com.w2w.api.employee.EmployeeRepository;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.scheduling.ScheduleRepository;
import com.w2w.api.scheduling.SchedulingService;
import com.w2w.api.scheduling.ShiftRepository;
import com.w2w.api.scheduling.dto.DayShiftBucket;
import com.w2w.api.scheduling.dto.EmployeeSchedule;
import com.w2w.api.scheduling.dto.GroupedShiftsResponse;
import com.w2w.api.scheduling.dto.ShiftGroup;
import com.w2w.api.scheduling.dto.ShiftGrouping;
import com.w2w.api.scheduling.dto.ShiftResponse;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.tenant.Company;
import com.w2w.api.tenant.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SchedulingServiceIT extends PostgresIntegrationTestBase {

    private static final Integer COMPANY_A_ID = 7001;
    private static final Integer COMPANY_B_ID = 7002;
    private static final Integer EMPLOYEE_A_ID = 700101;
    private static final Integer EMPLOYEE_B_ID = 700201;
    private static final LocalDate SHIFT_DATE = LocalDate.of(2026, 5, 1);
    private static final LocalDate UPDATED_SHIFT_DATE = LocalDate.of(2026, 5, 2);

    @Autowired
    private SchedulingService schedulingService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Test
    void saveShift_persistsTenantScopedShiftAndReusesExistingSchedule() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        createCompany(COMPANY_B_ID, "Pilot Tenant B");

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of("111-222"));
        Position position = createPosition(COMPANY_A_ID, "Bartender");
        Schedule existingSchedule = createSchedule(COMPANY_A_ID, SHIFT_DATE, true);

        Shift savedShift = schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Opening shift",
                SHIFT_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                position.getPositionId(),
                null,
                "amber"
        );

        assertNotNull(savedShift.getShiftId());
        assertEquals(COMPANY_A_ID, savedShift.getCompanyId());
        assertEquals(existingSchedule.getScheduleId(), savedShift.getScheduleId());
        assertEquals(8.0f, savedShift.getDuration());
        assertFalse(savedShift.getIsOvernight());
        assertEquals(EMPLOYEE_A_ID, savedShift.getChangedBy());
        assertEquals(1L, countSchedulesForTenantOnDate(COMPANY_A_ID, SHIFT_DATE));

        ShiftResponse shiftResponse = schedulingService.getShift(savedShift.getShiftId());

        assertEquals(savedShift.getShiftId(), shiftResponse.shiftId());
        assertEquals(EMPLOYEE_A_ID, shiftResponse.employeeId());
        assertEquals(COMPANY_A_ID, shiftResponse.companyId());
        assertEquals(SHIFT_DATE, shiftResponse.date());
        assertEquals(LocalTime.of(9, 0), shiftResponse.startTime());
        assertEquals(LocalTime.of(17, 0), shiftResponse.endTime());
        assertEquals("Bartender", shiftResponse.position());
        assertEquals("amber", shiftResponse.color());

        TenantContext.setCurrentTenant(COMPANY_B_ID);

        assertTrue(shiftRepository.findByShiftIdAndCompanyId(savedShift.getShiftId(), COMPANY_B_ID).isEmpty());
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.getShift(savedShift.getShiftId())
        );
        assertEquals("Shift not found with id: " + savedShift.getShiftId(), exception.getMessage());
    }

    @Test
    void saveShift_createsUnpublishedScheduleWhenMissing() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of());
        Position position = createPosition(COMPANY_A_ID, "Server");

        Shift savedShift = schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Closing shift",
                UPDATED_SHIFT_DATE,
                LocalTime.of(16, 0),
                LocalTime.of(22, 0),
                null,
                position.getPositionId(),
                null,
                "blue"
        );

        Schedule schedule = scheduleRepository.findByCompanyIdAndStartDate(COMPANY_A_ID, UPDATED_SHIFT_DATE)
                .orElseThrow();

        assertEquals(schedule.getScheduleId(), savedShift.getScheduleId());
        assertFalse(schedule.isPublished());
        assertEquals((short) UPDATED_SHIFT_DATE.getDayOfWeek().getValue(), schedule.getDayOfWeek());
        assertEquals(1L, countSchedulesForTenantOnDate(COMPANY_A_ID, UPDATED_SHIFT_DATE));
    }

    @Test
    void updateShift_updatesFieldsAndReusesSchedule() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of());
        Position originalPosition = createPosition(COMPANY_A_ID, "Bartender");
        Position updatedPosition = createPosition(COMPANY_A_ID, "Server");
        Category originalCategory = createCategory(COMPANY_A_ID, "Front", "FRT", originalPosition.getPositionId());
        Category updatedCategory = createCategory(COMPANY_A_ID, "Back", "BCK", updatedPosition.getPositionId());
        createSchedule(COMPANY_A_ID, SHIFT_DATE, true);
        Schedule updatedSchedule = createSchedule(COMPANY_A_ID, UPDATED_SHIFT_DATE, true);

        Shift savedShift = schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Opening shift",
                SHIFT_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                originalPosition.getPositionId(),
                originalCategory.getCategoryId(),
                "amber"
        );

        ShiftResponse updatedShift = schedulingService.updateShift(
                savedShift.getShiftId(),
                new UpdateShiftRequest(
                        savedShift.getShiftId(),
                        EMPLOYEE_A_ID,
                        "Updated shift",
                        LocalTime.of(10, 0),
                        LocalTime.of(18, 0),
                        updatedPosition.getPositionId(),
                        updatedCategory.getCategoryId(),
                        "blue",
                        UPDATED_SHIFT_DATE,
                        8.0f
                )
        );

        assertEquals(savedShift.getShiftId(), updatedShift.shiftId());
        assertEquals(UPDATED_SHIFT_DATE, updatedShift.date());
        assertEquals(LocalTime.of(10, 0), updatedShift.startTime());
        assertEquals(LocalTime.of(18, 0), updatedShift.endTime());
        assertEquals("Server", updatedShift.position());
        assertEquals("Back", updatedShift.category());
        assertEquals("blue", updatedShift.color());

        Shift persistedShift = shiftRepository.findByShiftIdAndCompanyId(savedShift.getShiftId(), COMPANY_A_ID)
                .orElseThrow();
        assertEquals(updatedSchedule.getScheduleId(), persistedShift.getScheduleId());
        assertEquals(updatedPosition.getPositionId(), persistedShift.getRequiredPositionId());
        assertEquals(updatedCategory.getCategoryId(), persistedShift.getCategoryId());
    }

    @Test
    void updateShift_rejectsCrossTenantShiftAccess() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        createCompany(COMPANY_B_ID, "Pilot Tenant B");
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of());
        Position position = createPosition(COMPANY_A_ID, "Bartender");

        Shift savedShift = schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Opening shift",
                SHIFT_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                position.getPositionId(),
                null,
                "amber"
        );

        TenantContext.setCurrentTenant(COMPANY_B_ID);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.updateShift(
                        savedShift.getShiftId(),
                        new UpdateShiftRequest(
                                savedShift.getShiftId(),
                                EMPLOYEE_B_ID,
                                "Cross-tenant update",
                                LocalTime.of(11, 0),
                                LocalTime.of(19, 0),
                                null,
                                null,
                                "red",
                                UPDATED_SHIFT_DATE,
                                8.0f
                        )
                )
        );

        assertEquals("Shift not found with id: " + savedShift.getShiftId(), exception.getMessage());

        TenantContext.setCurrentTenant(COMPANY_A_ID);
        ShiftResponse originalShift = schedulingService.getShift(savedShift.getShiftId());
        assertEquals("Opening shift", originalShift.description());
        assertEquals(SHIFT_DATE, originalShift.date());
    }

    @Test
    void softDeleteShift_hidesDeletedShiftFromDirectAndGroupedReads() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of("111-222"));
        Position position = createPosition(COMPANY_A_ID, "Bartender");
        assignEmployeeSkill(EMPLOYEE_A_ID, position.getPositionId());

        Shift savedShift = schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Opening shift",
                SHIFT_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                position.getPositionId(),
                null,
                "amber"
        );

        schedulingService.softDeleteShift(savedShift.getShiftId());

        Shift deletedShift = shiftRepository.findByShiftIdAndCompanyId(savedShift.getShiftId(), COMPANY_A_ID)
                .orElseThrow();
        assertTrue(deletedShift.getIsDeleted());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.getShift(savedShift.getShiftId())
        );
        assertEquals("Shift not found with id: " + savedShift.getShiftId(), exception.getMessage());

        List<EmployeeSchedule> groupedSchedules = schedulingService.getEmployeeShiftsGroupedInRange(SHIFT_DATE, SHIFT_DATE, null, null);
        assertEquals(1, groupedSchedules.size());
        assertEquals(0, groupedSchedules.getFirst().getShiftCount());
        assertTrue(groupedSchedules.getFirst().getWeeklyShifts().get(0).shifts().isEmpty());
    }

    @Test
    void getEmployeeShiftsGroupedInRange_splitsOvernightShift() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of("111-222", "333-444"));
        Position bartender = createPosition(COMPANY_A_ID, "Bartender");
        Position server = createPosition(COMPANY_A_ID, "Server");
        Category category = createCategory(COMPANY_A_ID, "Front", "FRT", bartender.getPositionId());
        assignEmployeeSkill(EMPLOYEE_A_ID, bartender.getPositionId());
        assignEmployeeSkill(EMPLOYEE_A_ID, server.getPositionId());

        schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Overnight shift",
                SHIFT_DATE,
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                null,
                bartender.getPositionId(),
                category.getCategoryId(),
                "purple"
        );

        List<EmployeeSchedule> schedules = schedulingService.getEmployeeShiftsGroupedInRange(
                SHIFT_DATE,
                SHIFT_DATE.plusDays(1),
                null,
                null
        );

        assertEquals(1, schedules.size());
        EmployeeSchedule employeeSchedule = schedules.getFirst();
        assertEquals(EMPLOYEE_A_ID, employeeSchedule.getEmployeeId());
        assertEquals(List.of("111-222", "333-444"), employeeSchedule.getPhones());
        assertEquals(1, employeeSchedule.getShiftCount());
        assertEquals(new BigDecimal("8.00"), employeeSchedule.getTotalHours());
        assertEquals(null, employeeSchedule.getEmploymentType());

        DayShiftBucket firstDayBucket = employeeSchedule.getWeeklyShifts().get(0);
        DayShiftBucket secondDayBucket = employeeSchedule.getWeeklyShifts().get(1);

        assertEquals("Friday May-01", firstDayBucket.date());
        assertEquals(1, firstDayBucket.shifts().size());
        assertEquals("10pm", firstDayBucket.shifts().getFirst().startTime());
        assertEquals("12am", firstDayBucket.shifts().getFirst().endTime());
        assertEquals(2.0f, firstDayBucket.shifts().getFirst().duration());

        assertEquals("Saturday May-02", secondDayBucket.date());
        assertEquals(1, secondDayBucket.shifts().size());
        assertEquals("12am", secondDayBucket.shifts().getFirst().startTime());
        assertEquals("6am", secondDayBucket.shifts().getFirst().endTime());
        assertEquals(6.0f, secondDayBucket.shifts().getFirst().duration());
    }

    @Test
    void getShiftsGrouped_returnsPositionTimingBucketsIncludingEmptyDates() {
        createCompany(COMPANY_A_ID, "Pilot Tenant A");
        TenantContext.setCurrentTenant(COMPANY_A_ID);
        createEmployee(EMPLOYEE_A_ID, COMPANY_A_ID, "Ava", "Stone", List.of("111-222"));
        Position position = createPosition(COMPANY_A_ID, "Bartender");

        schedulingService.saveShift(
                EMPLOYEE_A_ID,
                "Opening shift",
                SHIFT_DATE,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                position.getPositionId(),
                null,
                "amber"
        );

        GroupedShiftsResponse response = (GroupedShiftsResponse) schedulingService.getShiftsGrouped(
                SHIFT_DATE,
                SHIFT_DATE.plusDays(1),
                ShiftGrouping.POSITION_SHIFT_TIMINGS
        );

        assertEquals(2, response.dates().size());

        ShiftGroup firstDatePositionGroup = response.dates().getFirst().shiftGroups().getFirst();
        assertEquals("Bartender", firstDatePositionGroup.label());
        assertEquals(1, firstDatePositionGroup.shiftGroups().size());
        assertEquals("9:00AM-5:00PM", firstDatePositionGroup.shiftGroups().getFirst().label());
        assertEquals(1, firstDatePositionGroup.shiftGroups().getFirst().shifts().size());
        assertEquals("Opening shift", firstDatePositionGroup.shiftGroups().getFirst().shifts().getFirst().description());

        ShiftGroup secondDatePositionGroup = response.dates().get(1).shiftGroups().getFirst();
        assertEquals("Bartender", secondDatePositionGroup.label());
        assertTrue(secondDatePositionGroup.shiftGroups().isEmpty());
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
        if (companyRepository.existsById(companyId)) {
            return;
        }

        Company company = new Company();
        company.setCompanyId(companyId);
        company.setCompanyName(companyName);
        company.setDepartmentName("Integration");
        company.setStatus("active");
        company.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        companyRepository.save(company);
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

    private Category createCategory(Integer companyId, String description, String shortDesc, Integer skillId) {
        Category category = new Category();
        category.setCompanyId(companyId);
        category.setDescription(description);
        category.setShortDesc(shortDesc);
        category.setStartTime("08:00");
        category.setEndTime("17:00");
        category.setPositionId(skillId);
        category.setColor((short) 3);
        category.setIsDeleted(false);
        return categoryRepository.save(category);
    }

    private Schedule createSchedule(Integer companyId, LocalDate startDate, boolean isPublished) {
        Schedule schedule = new Schedule();
        schedule.setCompanyId(companyId);
        schedule.setDescription("Existing schedule");
        schedule.setPublished(isPublished);
        schedule.setStartDate(startDate);
        schedule.setDayOfWeek((short) DayOfWeek.from(startDate).getValue());
        schedule.setTimestamp(LocalDateTime.of(2026, 5, 1, 0, 0));
        return scheduleRepository.save(schedule);
    }

    private long countSchedulesForTenantOnDate(Integer companyId, LocalDate startDate) {
        TenantContext.setCurrentTenant(companyId);
        return scheduleRepository.findAll().stream()
                .filter(schedule -> companyId.equals(schedule.getCompanyId()))
                .filter(schedule -> startDate.equals(schedule.getStartDate()))
                .count();
    }
}
