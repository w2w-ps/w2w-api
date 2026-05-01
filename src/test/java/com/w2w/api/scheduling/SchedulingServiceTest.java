package com.w2w.api.scheduling;

import com.w2w.api.category.CategoryService;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.config.TenantContext;
import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.dto.*;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchedulingServiceTest {
    private SchedulingQueryRepository schedulingQueryRepository;
    private ShiftRepository shiftRepository;
    private ScheduleRepository scheduleRepository;
    private PositionService positionService;
    private CategoryService categoryService;
    private SchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        schedulingQueryRepository = Mockito.mock(SchedulingQueryRepository.class);
        shiftRepository = Mockito.mock(ShiftRepository.class);
        scheduleRepository = Mockito.mock(ScheduleRepository.class);
        positionService = Mockito.mock(PositionService.class);
        categoryService = Mockito.mock(CategoryService.class);
        ShiftCommandService shiftCommandService = new ShiftCommandService(shiftRepository, scheduleRepository);
        SchedulingGroupingService schedulingGroupingService = new SchedulingGroupingService(
                schedulingQueryRepository,
                positionService,
                categoryService
        );
        schedulingService = new SchedulingService(
                shiftCommandService,
                schedulingGroupingService,
                Mockito.mock(RuleEngineService.class)
        );
        TenantContext.setCurrentTenant(1);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getShiftColorsReturnsLegacyDropdownPalette() {
        List<ShiftColorResponse> colors = schedulingService.getShiftColors();

        assertEquals(17, colors.size());
        assertEquals(new ShiftColorResponse((short) 0, "black"), colors.get(0));
        assertEquals(new ShiftColorResponse((short) 1, "brown"), colors.get(1));
        assertEquals(new ShiftColorResponse((short) 16, "maroon"), colors.get(16));
    }

    @Test
    void saveShiftCalculatesDurationAndOvernightStatus() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest(
                101,
                1,
                null,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                1,
                null,
                (short) 1
        );

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );

        assertEquals(8.0f, saved.getDuration());
        assertEquals(false, saved.getIsOvernight());
        assertEquals(101, saved.getChangedBy());
        assertEquals(500, saved.getScheduleId());
        assertEquals((short) 1, saved.getColor());
    }

    @Test
    void saveShiftAllowsNullColor() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest(
                101,
                1,
                null,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                1,
                null,
                null
        );

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );

        assertNull(saved.getColor());
    }

    @Test
    void saveShiftCalculatesOvernightStatusCorrecty() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest(
                101,
                1,
                null,
                date,
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                null,
                1,
                null,
                null
        );

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );

        assertEquals(8.0f, saved.getDuration());
        assertEquals(true, saved.getIsOvernight());
    }

    @Test
    void saveShiftCreatesNewScheduleIfNotFound() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest(
                101,
                1,
                null,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                1,
                null,
                null
        );

        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule s = invocation.getArgument(0);
            s.setScheduleId(600);
            return s;
        });
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );

        assertEquals(600, saved.getScheduleId());
        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void saveShiftCreatesUnpublishedScheduleByDefault() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest(
                101,
                1,
                null,
                date,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                null,
                1,
                null,
                null
        );

        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setScheduleId(600);
            return schedule;
        });
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );

        ArgumentCaptor<Schedule> scheduleCaptor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertEquals(false, scheduleCaptor.getValue().isPublished());
    }

    @Test
    void saveShiftAlwaysDerivesOvernightFromTimes() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest(
                101,
                1,
                null,
                date,
                LocalTime.of(22, 0),
                LocalTime.of(6, 0),
                null,
                1,
                null,
                null
        );

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );

        assertEquals(8.0f, saved.getDuration());
        assertEquals(true, saved.getIsOvernight());
    }

    @Test
    void updateShiftUpdatesFieldsAndRecalculates() {
        Integer shiftId = 1001;
        UpdateShiftRequest request = new UpdateShiftRequest(
                shiftId,
                101,
                "New description",
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                2,
                null,
                (short) 1,
                null,
                null
        );

        Shift existingShift = new Shift();
        existingShift.setShiftId(shiftId);
        existingShift.setEmployeeId(101);
        existingShift.setCompanyId(1);
        existingShift.setStartTime(LocalTime.of(9, 0));
        existingShift.setEndTime(LocalTime.of(17, 0));
        existingShift.setDuration(8.0f);

        when(shiftRepository.findByShiftIdAndCompanyId(shiftId, 1)).thenReturn(Optional.of(existingShift));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shiftRepository.findShiftDetailsByShiftIdAndCompanyId(shiftId, 1))
                .thenReturn(Optional.of(new TestShiftDetailsProjection(
                        shiftId,
                        101,
                        1,
                        "New description",
                        null,
                        LocalTime.of(10, 0),
                        LocalTime.of(18, 0),
                        8.0f,
                        false,
                        "Bartender",
                        "Front",
                        (short) 1
                )));

        ShiftResponse updated = schedulingService.updateShift(shiftId, request);

        assertEquals("New description", updated.description());
        assertEquals(LocalTime.of(10, 0), updated.startTime());
        assertEquals(8.0f, updated.duration());
        assertEquals("Bartender", updated.position());
        assertEquals((short) 1, updated.color());
    }

    @Test
    void softDeleteShiftSetsIsDeletedToTrue() {
        Integer shiftId = 1001;
        Shift existingShift = new Shift();
        existingShift.setShiftId(shiftId);
        existingShift.setIsDeleted(false);

        when(shiftRepository.findByShiftIdAndCompanyId(shiftId, 1)).thenReturn(Optional.of(existingShift));

        schedulingService.softDeleteShift(shiftId);

        assertEquals(true, existingShift.getIsDeleted());
        verify(shiftRepository).save(existingShift);
    }

    @Test
    void getShiftReturnsSingleQueryDetailsProjection() {
        Integer shiftId = 1001;
        TenantContext.setCurrentTenant(7);
        when(shiftRepository.findShiftDetailsByShiftIdAndCompanyId(shiftId, 7))
                .thenReturn(Optional.of(new TestShiftDetailsProjection(
                        shiftId,
                        101,
                        7,
                        "Opening shift",
                        LocalDate.of(2026, 3, 31),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        8.0f,
                        false,
                        "Bartender",
                        "FRT",
                        (Short) null
                )));

        ShiftResponse response = schedulingService.getShift(shiftId);

        assertEquals(shiftId, response.shiftId());
        assertEquals(101, response.employeeId());
        assertEquals(7, response.companyId());
        assertEquals("Opening shift", response.description());
        assertEquals(LocalDate.of(2026, 3, 31), response.date());
        assertEquals(LocalTime.of(9, 0), response.startTime());
        assertEquals(LocalTime.of(17, 0), response.endTime());
        assertEquals(8.0f, response.duration());
        assertEquals(false, response.isOvernight());
        assertEquals("Bartender", response.position());
        assertEquals("FRT", response.category());
        assertNull(response.color());
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void getShiftThrowsWhenProjectionNotFound() {
        Integer shiftId = 1001;
        when(shiftRepository.findShiftDetailsByShiftIdAndCompanyId(shiftId, 1)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.getShift(shiftId)
        );

        assertEquals("Shift not found with id: 1001", exception.getMessage());
    }

    @Test
    void groupsShiftsRelativeToRequestedStartDateAndInitializesDayBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9001,
                        101,
                        "Ava",
                        "Stone",
                        List.of("111-222"),
                        List.of(new PositionSummary(12, "Bartender"), new PositionSummary(19, "Server")),
                        LocalDate.of(2026, 3, 25),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        false,
                        "Bartender",
                        "Front",
                        "Opening shift",
                        8.0f,
                        "brown"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate, null, null);

        assertEquals(1, result.size());
        EmployeeSchedule employee = result.getFirst();
        assertEquals(null, employee.getEmploymentType());

        assertEquals(3, employee.getWeeklyShifts().size());
        assertEquals("Wednesday Mar-25", employee.getWeeklyShifts().get(0).date());
        assertEquals("Thursday Mar-26", employee.getWeeklyShifts().get(1).date());
        assertEquals("Friday Mar-27", employee.getWeeklyShifts().get(2).date());

        DayShiftBucket firstBucket = employee.getWeeklyShifts().get(0);
        assertEquals(1, firstBucket.shifts().size());
        assertEquals(9001, firstBucket.shifts().getFirst().shiftId());
        assertEquals("9am", firstBucket.shifts().getFirst().startTime());
        assertEquals("5pm", firstBucket.shifts().getFirst().endTime());
        assertEquals("brown", firstBucket.shifts().getFirst().color());
        assertTrue(employee.getWeeklyShifts().get(1).shifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).shifts().isEmpty());
        assertEquals(new BigDecimal("8.00"), employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void splitsOvernightShiftsAcrossRelativeDateBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9002,
                        101,
                        "Ava",
                        "Stone",
                        List.of(),
                        List.of(new PositionSummary(12, "Bartender")),
                        LocalDate.of(2026, 3, 26),
                        LocalTime.of(22, 0),
                        LocalTime.of(6, 0),
                        true,
                        "Bartender",
                        "Front",
                        "Close",
                        8.0f,
                        "blue"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate, null, null);

        EmployeeSchedule employee = result.getFirst();
        assertEquals(3, employee.getWeeklyShifts().size());

        DayShiftBucket secondBucket = employee.getWeeklyShifts().get(1);
        DayShiftBucket thirdBucket = employee.getWeeklyShifts().get(2);

        assertEquals("Thursday Mar-26", secondBucket.date());
        assertEquals("Friday Mar-27", thirdBucket.date());
        assertEquals(1, secondBucket.shifts().size());
        assertEquals(1, thirdBucket.shifts().size());
        assertEquals(9002, secondBucket.shifts().getFirst().shiftId());
        assertEquals("10pm", secondBucket.shifts().getFirst().startTime());
        assertEquals("12am", secondBucket.shifts().getFirst().endTime());
        assertEquals("12am", thirdBucket.shifts().getFirst().startTime());
        assertEquals("6am", thirdBucket.shifts().getFirst().endTime());
        assertEquals("blue", secondBucket.shifts().getFirst().color());
        assertTrue(employee.getWeeklyShifts().get(1).shifts().getFirst().color().length() > 0);
        assertEquals(new BigDecimal("8.00"), employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void employeeGroupingUsesCategoryShortDescriptionAndPublishStatus() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9004,
                        101,
                        "Ava",
                        "Stone",
                        List.of("111-222"),
                        List.of(new PositionSummary(12, "Bartender")),
                        LocalDate.of(2026, 3, 25),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        false,
                        12,
                        "Bartender",
                        4,
                        "Front",
                        "FRT",
                        "Opening shift",
                        8.0f,
                        true,
                        "brown"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate, null, null);

        EmployeeSchedule employee = result.getFirst();
        assertEquals("Published", employee.getPublishedStage());
        assertEquals("FRT", employee.getWeeklyShifts().get(0).shifts().getFirst().category());
    }

    @Test
    void employeeGroupingAppliesPositionAndCategoryFiltersToEmployeesAndShifts() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(
                7,
                startDate.minusDays(1),
                endDate,
                List.of(12),
                List.of(4)
        ))
                .thenReturn(List.of(
                        new TestProjection(
                                9004,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                12,
                                "Bartender",
                                4,
                                "Front",
                                "FRT",
                                "Opening shift",
                                8.0f,
                                true,
                                "brown"
                        ),
                        new TestProjection(
                                9005,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                12,
                                "Bartender",
                                9,
                                "Floor",
                                "FLR",
                                "Lunch shift",
                                8.0f,
                                false,
                                "blue"
                        )
                ));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(
                startDate,
                endDate,
                List.of(12),
                List.of(4)
        );

        assertEquals(1, result.size());
        EmployeeSchedule employee = result.getFirst();
        assertEquals(101, employee.getEmployeeId());
        assertEquals(1, employee.getShiftCount());
        assertEquals(new BigDecimal("8.00"), employee.getTotalHours());
        assertEquals(2, employee.getWeeklyShifts().get(0).shifts().size());
        assertEquals("Bartender", employee.getWeeklyShifts().get(0).shifts().getFirst().position());
        assertEquals("FRT", employee.getWeeklyShifts().get(0).shifts().getFirst().category());
    }

    @Test
    void includesOnlyInRangeSegmentForOvernightShiftAtRangeBoundary() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9003,
                        101,
                        "Ava",
                        "Stone",
                        List.of(),
                        List.of(),
                        LocalDate.of(2026, 3, 24),
                        LocalTime.of(22, 0),
                        LocalTime.of(2, 0),
                        true,
                        "Bartender",
                        "Front",
                        "Carry over",
                        4.0f,
                        "fuchsia"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate, null, null);

        EmployeeSchedule employee = result.getFirst();
        assertNotNull(employee.getWeeklyShifts().get(0));
        assertEquals(1, employee.getWeeklyShifts().get(0).shifts().size());
        assertEquals(9003, employee.getWeeklyShifts().get(0).shifts().getFirst().shiftId());
        assertEquals("fuchsia", employee.getWeeklyShifts().get(0).shifts().getFirst().color());
        assertTrue(employee.getWeeklyShifts().get(0).shifts().getFirst().color().length() > 0);
        assertTrue(employee.getWeeklyShifts().get(1).shifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).shifts().isEmpty());
        assertEquals(new BigDecimal("2.00"), employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void groupedShiftsPreserveNullShiftIdAndColor() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        null,
                        101,
                        "Ava",
                        "Stone",
                        List.of(),
                        List.of(),
                        LocalDate.of(2026, 3, 25),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        false,
                        "Bartender",
                        "Front",
                        "No color shift",
                        8.0f,
                        null
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate, null, null);

        ShiftSummary groupedShift = result.getFirst().getWeeklyShifts().get(0).shifts().getFirst();
        assertEquals(null, groupedShift.shiftId());
        assertEquals("black", groupedShift.color());
    }

    @Test
    void groupsShiftsByDateAndPositionAndInitializesEmptyDateBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(
                        new PositionSummary(12, "Bartender"),
                        new PositionSummary(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                12,
                                "Bartender",
                                4,
                                "Front",
                                "FRT",
                                "Opening shift",
                                8.0f,
                                true,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                19,
                                "Server",
                                9,
                                "Floor",
                                "FLR",
                                "Lunch shift",
                                8.0f,
                                false,
                                "blue"
                        )
                ));

        DatePositionSummaryResponse result = schedulingService.getShiftsGroupedByDateAndPosition(startDate, endDate, null, null);

        assertEquals("Week of Mar-25", result.title());
        assertEquals(2, result.totalShifts());
        assertEquals(new BigDecimal("16.00"), result.totalHours());
        assertEquals(2, result.dates().size());
        assertEquals("Wednesday", result.dates().get(0).weekday());
        assertEquals("2026-03-25", result.dates().get(0).date());
        assertEquals(2, result.dates().get(0).shiftCount());
        assertEquals(new BigDecimal("16.00"), result.dates().get(0).totalDuration());
        assertEquals("Thursday", result.dates().get(1).weekday());
        assertEquals("2026-03-26", result.dates().get(1).date());
        assertEquals(0, result.dates().get(1).shiftCount());
        assertEquals(new BigDecimal("0.00"), result.dates().get(1).totalDuration());
        assertEquals(2, result.dates().get(1).positions().size());
        assertEquals("Bartender", result.dates().get(1).positions().get(0).position());
        assertEquals(0, result.dates().get(1).positions().get(0).shiftCount());
        assertEquals(new BigDecimal("0.00"), result.dates().get(1).positions().get(0).totalDuration());
        assertTrue(result.dates().get(1).positions().get(0).shifts().isEmpty());
        assertEquals("Server", result.dates().get(1).positions().get(1).position());
        assertEquals(0, result.dates().get(1).positions().get(1).shiftCount());
        assertEquals(new BigDecimal("0.00"), result.dates().get(1).positions().get(1).totalDuration());
        assertTrue(result.dates().get(1).positions().get(1).shifts().isEmpty());

        List<PositionShiftBucket> firstDayPositions = result.dates().get(0).positions();
        assertEquals(2, firstDayPositions.size());
        assertEquals("Bartender", firstDayPositions.get(0).position());
        assertEquals(1, firstDayPositions.get(0).shiftCount());
        assertEquals(new BigDecimal("8.00"), firstDayPositions.get(0).totalDuration());
        assertEquals(1, firstDayPositions.get(0).shifts().size());
        assertEquals("Server", firstDayPositions.get(1).position());
        assertEquals(1, firstDayPositions.get(1).shiftCount());
        assertEquals(new BigDecimal("8.00"), firstDayPositions.get(1).totalDuration());
        assertEquals(1, firstDayPositions.get(1).shifts().size());
        assertEquals(101, firstDayPositions.get(0).shifts().getFirst().employeeId());
        assertEquals("Ava", firstDayPositions.get(0).shifts().getFirst().firstName());
        assertEquals(null, firstDayPositions.get(0).shifts().getFirst().employmentType());
        assertEquals("9am", firstDayPositions.get(0).shifts().getFirst().startTime());
        assertEquals("FRT", firstDayPositions.get(0).shifts().getFirst().category());
        assertEquals("brown", firstDayPositions.get(0).shifts().getFirst().color());
        assertEquals(102, firstDayPositions.get(1).shifts().getFirst().employeeId());
        assertEquals("Lunch shift", firstDayPositions.get(1).shifts().getFirst().description());
    }

    @Test
    void dateAndPositionGroupingSplitsOvernightShiftAcrossMatchingBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(
                        new PositionSummary(12, "Bartender"),
                        new PositionSummary(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9003,
                        101,
                        "Ava",
                        "Stone",
                        List.of("111-222"),
                        List.of(new PositionSummary(12, "Bartender")),
                        LocalDate.of(2026, 3, 25),
                        LocalTime.of(22, 0),
                        LocalTime.of(6, 0),
                        true,
                        "Bartender",
                        "Front",
                        "Close",
                        8.0f,
                        "fuchsia"
                )));

        DatePositionSummaryResponse result = schedulingService.getShiftsGroupedByDateAndPosition(startDate, endDate, null, null);

        assertEquals(2, result.dates().size());
        assertEquals(2, result.dates().get(0).positions().size());
        assertEquals(2, result.dates().get(1).positions().size());
        assertEquals(2, result.totalShifts());
        assertEquals(new BigDecimal("8.00"), result.totalHours());
        assertEquals(1, result.dates().get(0).shiftCount());
        assertEquals(1, result.dates().get(1).shiftCount());
        assertEquals(new BigDecimal("2.00"), result.dates().get(0).totalDuration());
        assertEquals(new BigDecimal("6.00"), result.dates().get(1).totalDuration());
        assertEquals("Bartender", result.dates().get(0).positions().getFirst().position());
        assertEquals(1, result.dates().get(0).positions().getFirst().shiftCount());
        assertEquals(new BigDecimal("2.00"), result.dates().get(0).positions().getFirst().totalDuration());
        assertEquals(1, result.dates().get(0).positions().getFirst().shifts().size());
        assertEquals(9003, result.dates().get(0).positions().getFirst().shifts().getFirst().shiftId());
        assertEquals("10pm", result.dates().get(0).positions().getFirst().shifts().getFirst().startTime());
        assertEquals("12am", result.dates().get(0).positions().getFirst().shifts().getFirst().endTime());
        assertEquals(1, result.dates().get(1).positions().getFirst().shiftCount());
        assertEquals(new BigDecimal("6.00"), result.dates().get(1).positions().getFirst().totalDuration());
        assertEquals(1, result.dates().get(1).positions().getFirst().shifts().size());
        assertEquals("12am", result.dates().get(1).positions().getFirst().shifts().getFirst().startTime());
        assertEquals("6am", result.dates().get(1).positions().getFirst().shifts().getFirst().endTime());
        assertEquals("Server", result.dates().get(0).positions().get(1).position());
        assertEquals(0, result.dates().get(0).positions().get(1).shiftCount());
        assertEquals(new BigDecimal("0.00"), result.dates().get(0).positions().get(1).totalDuration());
        assertTrue(result.dates().get(0).positions().get(1).shifts().isEmpty());
        assertEquals("Server", result.dates().get(1).positions().get(1).position());
        assertEquals(0, result.dates().get(1).positions().get(1).shiftCount());
        assertEquals(new BigDecimal("0.00"), result.dates().get(1).positions().get(1).totalDuration());
        assertTrue(result.dates().get(1).positions().get(1).shifts().isEmpty());
    }

    @Test
    void dateAndPositionGroupingAppliesPositionAndCategoryFilters() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(
                        new PositionSummary(12, "Bartender"),
                        new PositionSummary(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(
                7,
                startDate.minusDays(1),
                endDate,
                List.of(12),
                List.of(4)
        ))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                12,
                                "Bartender",
                                4,
                                "Front",
                                "FRT",
                                "Opening shift",
                                8.0f,
                                true,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                19,
                                "Server",
                                9,
                                "Floor",
                                "FLR",
                                "Lunch shift",
                                8.0f,
                                false,
                                "blue"
                        )
                ));

        DatePositionSummaryResponse result = schedulingService.getShiftsGroupedByDateAndPosition(
                startDate,
                endDate,
                List.of(12),
                List.of(4)
        );

        assertEquals(1, result.totalShifts());
        assertEquals(new BigDecimal("8.00"), result.totalHours());
        assertEquals(1, result.dates().getFirst().positions().size());
        assertEquals("Bartender", result.dates().getFirst().positions().getFirst().position());
        assertEquals(1, result.dates().getFirst().positions().getFirst().shiftCount());
        assertEquals("Wednesday", result.dates().getFirst().weekday());
        assertEquals("2026-03-25", result.dates().getFirst().date());
        assertEquals("Wednesday - Mar 25, 2026", result.title());
    }

    @Test
    void dateAndPositionGroupingSortsShiftsByLastNameAscendingWithinPosition() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(new PositionSummary(12, "Bartender")));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Zulu",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                12,
                                "Bartender",
                                4,
                                "Front",
                                "FRT",
                                "Opening shift",
                                8.0f,
                                true,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Alpha",
                                List.of("333-444"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                12,
                                "Bartender",
                                4,
                                "Front",
                                "FRT",
                                "Lunch shift",
                                8.0f,
                                true,
                                "blue"
                        )
                ));

        DatePositionSummaryResponse result = schedulingService.getShiftsGroupedByDateAndPosition(startDate, endDate, null, null);

        List<EmployeeShift> shifts = result.dates().getFirst().positions().getFirst().shifts();
        assertEquals(2, shifts.size());
        assertEquals(null, shifts.get(0).employmentType());
        assertEquals("Alpha", shifts.get(0).lastName());
        assertEquals("Zulu", shifts.get(1).lastName());
    }

    @Test
    void dayPositionTimingGroupingGroupsShiftsByTimeWindow() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(
                        new PositionSummary(12, "Bartender"),
                        new PositionSummary(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Opening shift",
                                8.0f,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                "Server",
                                "Floor",
                                "Lunch shift",
                                8.0f,
                                "blue"
                        )
                ));

        List<DayPositionTimingBucketDto> result = schedulingService.getShiftsGroupedByDayPositionAndTiming(startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).positions().get(0).shiftTimings().size());
        assertEquals(LocalTime.of(9, 0), result.get(0).positions().get(0).shiftTimings().getFirst().startTime());
        assertEquals(LocalTime.of(17, 0), result.get(0).positions().get(0).shiftTimings().getFirst().endTime());
        assertEquals(101, result.get(0).positions().get(0).shiftTimings().getFirst().shifts().getFirst().employeeId());
        assertTrue(result.get(1).positions().get(0).shiftTimings().isEmpty());
    }

    @Test
    void dayCategoryTimingGroupingGroupsShiftsByCategoryAndTimeWindow() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);
        TenantContext.setCurrentTenant(7);

        when(categoryService.getCategoriesByCompanyId())
                .thenReturn(List.of(
                        new CategorySummary(4, "Floor", "FL"),
                        new CategorySummary(5, "Front", "FR")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Opening shift",
                                8.0f,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                "Server",
                                "Floor",
                                "Lunch shift",
                                8.0f,
                                "blue"
                        )
                ));

        List<DayCategoryTimingBucketDto> result = schedulingService.getShiftsGroupedByDayCategoryAndTiming(startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).categories().size());
        assertEquals("Floor", result.get(0).categories().get(0).category());
        assertEquals(1, result.get(0).categories().get(0).shiftTimings().size());
        assertEquals("Front", result.get(0).categories().get(1).category());
        assertEquals(1, result.get(0).categories().get(1).shiftTimings().size());
        assertTrue(result.get(1).categories().get(0).shiftTimings().isEmpty());
        assertTrue(result.get(1).categories().get(1).shiftTimings().isEmpty());
    }

    @Test
    void dayCategoryShortNameTimingGroupingUsesCategoryShortDescriptions() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);
        TenantContext.setCurrentTenant(7);

        when(categoryService.getCategoriesByCompanyId())
                .thenReturn(List.of(
                        new CategorySummary(1, "Floor", "FLR"),
                        new CategorySummary(2, "Front", "FRT")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Opening shift",
                                8.0f,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                "Server",
                                "Floor",
                                "Lunch shift",
                                8.0f,
                                "blue"
                        )
                ));

        List<DayCategoryTimingBucketDto> result =
                schedulingService.getShiftsGroupedByDayCategoryShortNameAndTiming(startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).categories().size());
        assertEquals("FLR", result.get(0).categories().get(0).category());
        assertEquals(1, result.get(0).categories().get(0).shiftTimings().size());
        assertEquals("FRT", result.get(0).categories().get(1).category());
        assertEquals(1, result.get(0).categories().get(1).shiftTimings().size());
        assertTrue(result.get(1).categories().get(0).shiftTimings().isEmpty());
        assertTrue(result.get(1).categories().get(1).shiftTimings().isEmpty());
    }

    @Test
    void getShiftsGrouped_returnsNormalizedPositionShiftTimingResponse() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(new PositionSummary(12, "Bartender")));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Opening shift",
                                8.0f,
                                "brown"
                        )
                ));

        GroupedShiftsResponse result = schedulingService.getShiftsGrouped(startDate, endDate, ShiftGrouping.POSITION_SHIFT_TIMINGS);

        assertEquals(1, result.dates().size());
        assertEquals("Bartender", result.dates().getFirst().shiftGroups().getFirst().label());
        assertEquals(1, result.dates().getFirst().shiftGroups().getFirst().shiftGroups().size());
        assertEquals("9am-5pm", result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().label());
        assertEquals(1, result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().shifts().size());
        assertEquals(
                "9am",
                result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().shifts().getFirst().startTime()
        );
        assertEquals(
                "5pm",
                result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().shifts().getFirst().endTime()
        );
        assertEquals(
                "Bartender",
                result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().shifts().getFirst().position()
        );
    }

    @Test
    void getShiftsGrouped_positionShiftTimingsAppliesPositionAndCategoryFilters() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(positionService.get("all"))
                .thenReturn(List.of(
                        new PositionSummary(12, "Bartender"),
                        new PositionSummary(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(
                7,
                startDate.minusDays(1),
                endDate,
                List.of(12),
                List.of(4)
        )).thenReturn(twoFilteredCandidateRows(startDate));

        GroupedShiftsResponse result = schedulingService.getShiftsGrouped(
                startDate,
                endDate,
                ShiftGrouping.POSITION_SHIFT_TIMINGS,
                List.of(12),
                List.of(4)
        );

        List<ShiftGroup> positions = result.dates().getFirst().shiftGroups();
        assertEquals(1, positions.size());
        assertEquals("Bartender", positions.getFirst().label());
        assertEquals(1, positions.getFirst().shiftGroups().size());
        assertEquals(9001, positions.getFirst().shiftGroups().getFirst().shifts().getFirst().shiftId());
        assertEquals("Bartender", positions.getFirst().shiftGroups().getFirst().shifts().getFirst().position());
        assertEquals("FRT", positions.getFirst().shiftGroups().getFirst().shifts().getFirst().category());
    }

    @Test
    void getShiftsGrouped_shiftTimingsAppliesPositionAndCategoryFilters() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(
                7,
                startDate.minusDays(1),
                endDate,
                List.of(12),
                List.of(4)
        )).thenReturn(twoFilteredCandidateRows(startDate));

        GroupedShiftsResponse result = schedulingService.getShiftsGrouped(
                startDate,
                endDate,
                ShiftGrouping.SHIFT_TIMINGS,
                List.of(12),
                List.of(4)
        );

        List<ShiftGroup> timingGroups = result.dates().getFirst().shiftGroups();
        assertEquals(1, timingGroups.size());
        assertEquals("9am-5pm", timingGroups.getFirst().label());
        assertEquals(1, timingGroups.getFirst().shifts().size());
        assertEquals(9001, timingGroups.getFirst().shifts().getFirst().shiftId());
        assertEquals("9am", timingGroups.getFirst().shifts().getFirst().startTime());
        assertEquals("5pm", timingGroups.getFirst().shifts().getFirst().endTime());
        assertEquals("Bartender", timingGroups.getFirst().shifts().getFirst().position());
        assertEquals("FRT", timingGroups.getFirst().shifts().getFirst().category());
    }

    @Test
    void getShiftsGrouped_categoryShiftTimingsAppliesFiltersAndInitializesMatchingCategories() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(categoryService.getCategoriesByCompanyId())
                .thenReturn(List.of(
                        new CategorySummary(4, "Front", "FRT"),
                        new CategorySummary(9, "Floor", "FLR")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(
                7,
                startDate.minusDays(1),
                endDate,
                List.of(12),
                List.of(4)
        )).thenReturn(twoFilteredCandidateRows(startDate));

        GroupedShiftsResponse result = schedulingService.getShiftsGrouped(
                startDate,
                endDate,
                ShiftGrouping.CATEGORY_SHIFT_TIMINGS,
                List.of(12),
                List.of(4)
        );

        List<ShiftGroup> categories = result.dates().getFirst().shiftGroups();
        assertEquals(1, categories.size());
        assertEquals("Front", categories.getFirst().label());
        assertEquals(1, categories.getFirst().shiftGroups().size());
        assertEquals(9001, categories.getFirst().shiftGroups().getFirst().shifts().getFirst().shiftId());
        assertEquals("Bartender", categories.getFirst().shiftGroups().getFirst().shifts().getFirst().position());
        assertEquals("FRT", categories.getFirst().shiftGroups().getFirst().shifts().getFirst().category());
    }

    @Test
    void getShiftsGrouped_categoryShiftTimingsDoesNotCreateShortCategoryBucket() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(categoryService.getCategoriesByCompanyId())
                .thenReturn(List.of(new CategorySummary(4, "Front", "FRT")));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9001,
                        101,
                        "Ava",
                        "Stone",
                        List.of("111-222"),
                        List.of(new PositionSummary(12, "Bartender")),
                        startDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        false,
                        12,
                        "Bartender",
                        4,
                        "Front",
                        "FRT",
                        "Opening shift",
                        8.0f,
                        true,
                        "brown"
                )));

        GroupedShiftsResponse result = schedulingService.getShiftsGrouped(
                startDate,
                endDate,
                ShiftGrouping.CATEGORY_SHIFT_TIMINGS
        );

        List<String> labels = result.dates().getFirst().shiftGroups().stream()
                .map(ShiftGroup::label)
                .toList();
        assertEquals(List.of("Front"), labels);
        assertFalse(labels.contains("FRT"));
        assertEquals(
                "FRT",
                result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().shifts().getFirst().category()
        );
    }

    @Test
    void getShiftsGrouped_catShiftTimingsAppliesFiltersAndInitializesMatchingShortCategories() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(categoryService.getCategoriesByCompanyId())
                .thenReturn(List.of(
                        new CategorySummary(4, "Front", "FRT"),
                        new CategorySummary(9, "Floor", "FLR")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(
                7,
                startDate.minusDays(1),
                endDate,
                List.of(12),
                List.of(4)
        )).thenReturn(twoFilteredCandidateRows(startDate));

        GroupedShiftsResponse result = schedulingService.getShiftsGrouped(
                startDate,
                endDate,
                ShiftGrouping.CAT_SHIFT_TIMINGS,
                List.of(12),
                List.of(4)
        );

        List<ShiftGroup> categories = result.dates().getFirst().shiftGroups();
        assertEquals(1, categories.size());
        assertEquals("FRT", categories.getFirst().label());
        assertEquals(1, categories.getFirst().shiftGroups().size());
        assertEquals(9001, categories.getFirst().shiftGroups().getFirst().shifts().getFirst().shiftId());
        assertEquals("Bartender", categories.getFirst().shiftGroups().getFirst().shifts().getFirst().position());
        assertEquals("FRT", categories.getFirst().shiftGroups().getFirst().shifts().getFirst().category());
    }

    @Test
    void getShiftsGrouped_returnsNormalizedCategoryShortNameResponse() {
        TenantContext.setCurrentTenant(7);
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);

        when(categoryService.getCategoriesByCompanyId())
                .thenReturn(List.of(new CategorySummary(2, "Front", "FRT")));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Opening shift",
                                8.0f,
                                "brown"
                        )
                ));

        GroupedShiftsResponse result =
                (GroupedShiftsResponse) schedulingService.getShiftsGrouped(startDate, endDate, ShiftGrouping.CAT_SHIFT_TIMINGS);

        ShiftGroup categoryGroup = result.dates().getFirst().shiftGroups().getFirst();
        assertEquals("FRT", categoryGroup.label());
        assertEquals(1, categoryGroup.shiftGroups().size());
        assertEquals("9am-5pm", categoryGroup.shiftGroups().getFirst().label());
        assertEquals(1, categoryGroup.shiftGroups().getFirst().shifts().size());
    }

    @Test
    void dayShiftTimingGroupingGroupsShiftsByTimeWindowWithoutAggregates() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Opening shift",
                                8.0f,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Server",
                                "Floor",
                                "Lunch shift",
                                8.0f,
                                "blue"
                        )
                ));

        List<DayShiftTimingBucketDto> result = schedulingService.getShiftsGroupedByDayAndTiming(startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(startDate, result.get(0).date());
        assertEquals(1, result.get(0).shiftTimings().size());
        assertEquals("9am-5pm", result.get(0).shiftTimings().getFirst().label());
        assertEquals(2, result.get(0).shiftTimings().getFirst().shifts().size());
        assertTrue(result.get(1).shiftTimings().isEmpty());
    }

    @Test
    void dayShiftTimingGroupingOrdersBucketsByTimeNotPosition() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionSummary(12, "Bartender")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(10, 0),
                                LocalTime.of(18, 0),
                                false,
                                "Bartender",
                                "Front",
                                "Late shift",
                                8.0f,
                                "brown"
                        ),
                        new TestProjection(
                                9002,
                                102,
                                "Ben",
                                "Cole",
                                List.of("333-444"),
                                List.of(new PositionSummary(19, "Server")),
                                LocalDate.of(2026, 3, 25),
                                LocalTime.of(9, 0),
                                LocalTime.of(17, 0),
                                false,
                                "Server",
                                "Floor",
                                "Early shift",
                                8.0f,
                                "blue"
                        )
                ));

        List<DayShiftTimingBucketDto> result = schedulingService.getShiftsGroupedByDayAndTiming(startDate, endDate);

        assertEquals(2, result.getFirst().shiftTimings().size());
        assertEquals("9am-5pm", result.getFirst().shiftTimings().get(0).label());
        assertEquals("10am-6pm", result.getFirst().shiftTimings().get(1).label());
    }

    @Test
    void includesUnassignedShiftsInGroupedResults() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);
        TenantContext.setCurrentTenant(7);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9005,
                        null, // Unassigned
                        null,
                        null,
                        List.of(),
                        List.of(),
                        LocalDate.of(2026, 3, 25),
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        false,
                        "Bartender",
                        "Front",
                        "Open Shift",
                        8.0f,
                        "gray"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate, null, null);

        assertEquals(1, result.size());
        EmployeeSchedule unassignedEntry = result.getFirst();
        assertEquals(null, unassignedEntry.getEmployeeId());
        assertEquals(1, unassignedEntry.getWeeklyShifts().get(0).shifts().size());
        assertEquals(9005, unassignedEntry.getWeeklyShifts().get(0).shifts().getFirst().shiftId());
    }

    private List<EmployeeShiftProjection> twoFilteredCandidateRows(LocalDate shiftDate) {
        return List.of(
                new TestProjection(
                        9001,
                        101,
                        "Ava",
                        "Stone",
                        List.of("111-222"),
                        List.of(new PositionSummary(12, "Bartender")),
                        shiftDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        false,
                        12,
                        "Bartender",
                        4,
                        "Front",
                        "FRT",
                        "Opening shift",
                        8.0f,
                        true,
                        "brown"
                ),
                new TestProjection(
                        9002,
                        102,
                        "Ben",
                        "Cole",
                        List.of("333-444"),
                        List.of(new PositionSummary(19, "Server")),
                        shiftDate,
                        LocalTime.of(10, 0),
                        LocalTime.of(18, 0),
                        false,
                        19,
                        "Server",
                        9,
                        "Floor",
                        "FLR",
                        "Lunch shift",
                        8.0f,
                        false,
                        "blue"
                )
        );
    }

    private record TestProjection(
            Integer shiftId,
            Integer employeeId,
            String firstName,
            String lastName,
            String employmentType,
            List<String> phones,
            List<PositionSummary> availablePositions,
            LocalDate weekCommencing,
            LocalTime startTime,
            LocalTime endTime,
            Boolean isOvernight,
            Integer positionId,
            String position,
            Integer categoryId,
            String category,
            String categoryShortDescription,
            String description,
            Float duration,
            Boolean schedulePublished,
            Short color
    ) implements EmployeeShiftProjection {
        private TestProjection(
                Integer shiftId,
                Integer employeeId,
                String firstName,
                String lastName,
                List<String> phones,
                List<PositionSummary> availablePositions,
                LocalDate weekCommencing,
                LocalTime startTime,
                LocalTime endTime,
                Boolean isOvernight,
                Integer positionId,
                String position,
                Integer categoryId,
                String category,
                String categoryShortDescription,
                String description,
                Float duration,
                Boolean schedulePublished,
                String color
        ) {
            this(
                    shiftId,
                    employeeId,
                    firstName,
                    lastName,
                    "Part Time",
                    phones,
                    availablePositions,
                    weekCommencing,
                    startTime,
                    endTime,
                    isOvernight,
                    positionId,
                    position,
                    categoryId,
                    category,
                    categoryShortDescription,
                    description,
                    duration,
                    schedulePublished,
                    toColorId(color)
            );
        }

        private TestProjection(
                Integer shiftId,
                Integer employeeId,
                String firstName,
                String lastName,
                List<String> phones,
                List<PositionSummary> availablePositions,
                LocalDate weekCommencing,
                LocalTime startTime,
                LocalTime endTime,
                Boolean isOvernight,
                String position,
                String category,
                String description,
                Float duration,
                String color
        ) {
            this(
                    shiftId,
                    employeeId,
                    firstName,
                    lastName,
                    "Part Time",
                    phones,
                    availablePositions,
                    weekCommencing,
                    startTime,
                    endTime,
                    isOvernight,
                    null,
                    position,
                    null,
                    category,
                    category,
                    description,
                    duration,
                    null,
                    toColorId(color)
            );
        }

        @Override
        public Integer getShiftId() { return shiftId; }
        @Override
        public Integer getEmployeeId() { return employeeId; }
        @Override
        public String getFirstName() { return firstName; }
        @Override
        public String getLastName() { return lastName; }
        @Override
        public String getEmploymentType() { return employmentType; }
        @Override
        public List<String> getPhones() { return phones; }
        @Override
        public List<PositionSummary> getAvailablePositions() { return availablePositions; }
        @Override
        public LocalDate getWeekCommencing() { return weekCommencing; }
        @Override
        public LocalTime getStartTime() { return startTime; }
        @Override
        public LocalTime getEndTime() { return endTime; }
        @Override
        public Integer getPositionId() { return positionId; }
        @Override
        public String getPosition() { return position; }
        @Override
        public Integer getCategoryId() { return categoryId; }
        @Override
        public String getCategory() { return category; }
        @Override
        public String getCategoryShortDescription() { return categoryShortDescription; }
        @Override
        public String getDescription() { return description; }
        @Override
        public Float getDuration() { return duration; }
        @Override
        public Boolean getIsOvernight() { return isOvernight; }
        @Override
        public Boolean getSchedulePublished() { return schedulePublished; }
        @Override
        public Short getColor() { return color; }
    }

    private record TestShiftDetailsProjection(
            Integer shiftId,
            Integer employeeId,
            Integer companyId,
            String description,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Float duration,
            Boolean isOvernight,
            String position,
            String category,
            Short color
    ) implements ShiftDetailsProjection {
        private TestShiftDetailsProjection(
                Integer shiftId,
                Integer employeeId,
                Integer companyId,
                String description,
                LocalDate date,
                LocalTime startTime,
                LocalTime endTime,
                Float duration,
                Boolean isOvernight,
                String position,
                String category,
                String color
        ) {
            this(
                    shiftId,
                    employeeId,
                    companyId,
                    description,
                    date,
                    startTime,
                    endTime,
                    duration,
                    isOvernight,
                    position,
                    category,
                    toColorId(color)
            );
        }

        @Override
        public Integer getShiftId() { return shiftId; }
        @Override
        public Integer getEmployeeId() { return employeeId; }
        @Override
        public Integer getCompanyId() { return companyId; }
        @Override
        public String getDescription() { return description; }
        @Override
        public LocalDate getDate() { return date; }
        @Override
        public LocalTime getStartTime() { return startTime; }
        @Override
        public LocalTime getEndTime() { return endTime; }
        @Override
        public Float getDuration() { return duration; }
        @Override
        public Boolean getIsOvernight() { return isOvernight; }
        @Override
        public String getPosition() { return position; }
        @Override
        public String getCategory() { return category; }
        @Override
        public Short getColor() { return color; }
    }

    private static Short toColorId(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        return switch (color) {
            case "brown" -> (short) 1;
            case "blue" -> (short) 2;
            case "fuchsia" -> (short) 3;
            case "gray", "open" -> (short) 4;
            default -> (short) 0;
        };
    }
}
