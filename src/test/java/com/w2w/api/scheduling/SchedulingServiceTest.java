package com.w2w.api.scheduling;

import com.w2w.api.category.CategoryService;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.scheduling.dto.CategoryTimingBucket;
import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.DayPositionBucket;
import com.w2w.api.scheduling.dto.DayShiftBucket;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.EmployeeSchedule;
import com.w2w.api.scheduling.dto.PositionShiftBucket;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.ShiftResponse;
import com.w2w.api.scheduling.dto.DayCategoryTimingBucket;
import com.w2w.api.scheduling.dto.DayPositionTimingBucket;
import com.w2w.api.scheduling.dto.DayShiftTimingBucket;
import com.w2w.api.scheduling.dto.GroupedShiftsResponse;
import com.w2w.api.scheduling.dto.PositionTimingBucket;
import com.w2w.api.scheduling.dto.ShiftGroup;
import com.w2w.api.scheduling.dto.ShiftGrouping;
import com.w2w.api.scheduling.dto.ShiftSummary;
import com.w2w.api.scheduling.dto.ShiftTimingBucket;
import com.w2w.api.scheduling.dto.ShiftTimingGroup;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        schedulingService = new SchedulingService();
        ReflectionTestUtils.setField(schedulingService, "schedulingQueryRepository", schedulingQueryRepository);
        ReflectionTestUtils.setField(schedulingService, "shiftRepository", shiftRepository);
        ReflectionTestUtils.setField(schedulingService, "scheduleRepository", scheduleRepository);
        ReflectionTestUtils.setField(schedulingService, "positionService", positionService);
        ReflectionTestUtils.setField(schedulingService, "categoryService", categoryService);
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
                "amber"
        );

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(request);

        assertEquals(8.0f, saved.getDuration());
        assertEquals(false, saved.getIsOvernight());
        assertEquals(101, saved.getChangedBy());
        assertEquals(500, saved.getScheduleId());
        assertEquals("amber", saved.getColor());
    }

    @Test
    void saveShiftAllowsBlankColor() {
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
                ""
        );

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(request);

        assertEquals("", saved.getColor());
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

        Shift saved = schedulingService.saveShift(request);

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

        Shift saved = schedulingService.saveShift(request);

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

        schedulingService.saveShift(request);

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

        Shift saved = schedulingService.saveShift(request);

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
                "amber",
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

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(existingShift));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shiftRepository.findShiftDetailsByShiftId(shiftId))
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
                        "amber"
                )));

        ShiftResponse updated = schedulingService.updateShift(shiftId, request);

        assertEquals("New description", updated.description());
        assertEquals(LocalTime.of(10, 0), updated.startTime());
        assertEquals(8.0f, updated.duration());
        assertEquals("Bartender", updated.position());
        assertEquals("amber", updated.color());
    }

    @Test
    void softDeleteShiftSetsIsDeletedToTrue() {
        Integer shiftId = 1001;
        Shift existingShift = new Shift();
        existingShift.setShiftId(shiftId);
        existingShift.setIsDeleted(false);

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(existingShift));

        schedulingService.softDeleteShift(shiftId);

        assertEquals(true, existingShift.getIsDeleted());
        verify(shiftRepository).save(existingShift);
    }

    @Test
    void getShiftReturnsSingleQueryDetailsProjection() {
        Integer shiftId = 1001;
        when(shiftRepository.findShiftDetailsByShiftId(shiftId))
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
                        "Front",
                        ""
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
        assertEquals("Front", response.category());
        assertEquals("", response.color());
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void getShiftThrowsWhenProjectionNotFound() {
        Integer shiftId = 1001;
        when(shiftRepository.findShiftDetailsByShiftId(shiftId)).thenReturn(Optional.empty());

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
                        "amber"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        assertEquals(1, result.size());
        EmployeeSchedule employee = result.getFirst();
        assertEquals(2, employee.getAvailablePositions().size());
        assertEquals(12, employee.getAvailablePositions().getFirst().id());
        assertEquals("Bartender", employee.getAvailablePositions().getFirst().name());

        assertEquals(3, employee.getWeeklyShifts().size());
        assertEquals(startDate, employee.getWeeklyShifts().get(0).date());
        assertEquals(startDate.plusDays(1), employee.getWeeklyShifts().get(1).date());
        assertEquals(startDate.plusDays(2), employee.getWeeklyShifts().get(2).date());

        DayShiftBucket firstBucket = employee.getWeeklyShifts().get(0);
        assertEquals(1, firstBucket.shifts().size());
        assertEquals(9001, firstBucket.shifts().getFirst().shiftId());
        assertEquals("amber", firstBucket.shifts().getFirst().color());
        assertTrue(employee.getWeeklyShifts().get(1).shifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).shifts().isEmpty());
        assertEquals(8.0, employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void splitsOvernightShiftsAcrossRelativeDateBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);

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

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        EmployeeSchedule employee = result.getFirst();
        assertEquals(3, employee.getWeeklyShifts().size());

        DayShiftBucket secondBucket = employee.getWeeklyShifts().get(1);
        DayShiftBucket thirdBucket = employee.getWeeklyShifts().get(2);

        assertEquals(LocalDate.of(2026, 3, 26), secondBucket.date());
        assertEquals(LocalDate.of(2026, 3, 27), thirdBucket.date());
        assertEquals(1, secondBucket.shifts().size());
        assertEquals(1, thirdBucket.shifts().size());
        assertEquals(9002, secondBucket.shifts().getFirst().shiftId());
        assertEquals("blue", secondBucket.shifts().getFirst().color());
        assertTrue(employee.getWeeklyShifts().get(1).shifts().getFirst().color().length() > 0);
        assertEquals(8.0, employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void includesOnlyInRangeSegmentForOvernightShiftAtRangeBoundary() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);

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
                        "charcoal"
                )));

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        EmployeeSchedule employee = result.getFirst();
        assertNotNull(employee.getWeeklyShifts().get(0));
        assertEquals(1, employee.getWeeklyShifts().get(0).shifts().size());
        assertEquals(9003, employee.getWeeklyShifts().get(0).shifts().getFirst().shiftId());
        assertEquals("charcoal", employee.getWeeklyShifts().get(0).shifts().getFirst().color());
        assertTrue(employee.getWeeklyShifts().get(0).shifts().getFirst().color().length() > 0);
        assertTrue(employee.getWeeklyShifts().get(1).shifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).shifts().isEmpty());
        assertEquals(2.0, employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void groupedShiftsPreserveNullShiftIdAndColor() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);

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

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        ShiftSummary groupedShift = result.getFirst().getWeeklyShifts().get(0).shifts().getFirst();
        assertEquals(null, groupedShift.shiftId());
        assertEquals(null, groupedShift.color());
    }

    @Test
    void groupsShiftsByDayAndPositionAndInitializesEmptyDayBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);

        when(positionService.getPositionsByCompanyId(7))
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
                                "amber"
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

        List<DayPositionBucket> result = schedulingService.getShiftsGroupedByDayAndPosition(7, startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(startDate, result.get(0).date());
        assertEquals(2, result.get(0).shiftCount());
        assertEquals(16.0f, result.get(0).totalDuration());
        assertEquals(endDate, result.get(1).date());
        assertEquals(0, result.get(1).shiftCount());
        assertEquals(0.0f, result.get(1).totalDuration());
        assertEquals(2, result.get(1).positions().size());
        assertEquals("Bartender", result.get(1).positions().get(0).position());
        assertEquals(0, result.get(1).positions().get(0).shiftCount());
        assertEquals(0.0f, result.get(1).positions().get(0).totalDuration());
        assertTrue(result.get(1).positions().get(0).shifts().isEmpty());
        assertEquals("Server", result.get(1).positions().get(1).position());
        assertEquals(0, result.get(1).positions().get(1).shiftCount());
        assertEquals(0.0f, result.get(1).positions().get(1).totalDuration());
        assertTrue(result.get(1).positions().get(1).shifts().isEmpty());

        List<PositionShiftBucket> firstDayPositions = result.get(0).positions();
        assertEquals(2, firstDayPositions.size());
        assertEquals("Bartender", firstDayPositions.get(0).position());
        assertEquals(1, firstDayPositions.get(0).shiftCount());
        assertEquals(8.0f, firstDayPositions.get(0).totalDuration());
        assertEquals(1, firstDayPositions.get(0).shifts().size());
        assertEquals("Server", firstDayPositions.get(1).position());
        assertEquals(1, firstDayPositions.get(1).shiftCount());
        assertEquals(8.0f, firstDayPositions.get(1).totalDuration());
        assertEquals(1, firstDayPositions.get(1).shifts().size());
        assertEquals(101, firstDayPositions.get(0).shifts().getFirst().employeeId());
        assertEquals("Ava", firstDayPositions.get(0).shifts().getFirst().firstName());
        assertEquals("amber", firstDayPositions.get(0).shifts().getFirst().color());
        assertEquals(102, firstDayPositions.get(1).shifts().getFirst().employeeId());
        assertEquals("Lunch shift", firstDayPositions.get(1).shifts().getFirst().description());
    }

    @Test
    void dayAndPositionGroupingSplitsOvernightShiftAcrossMatchingBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);

        when(positionService.getPositionsByCompanyId(7))
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
                        "charcoal"
                )));

        List<DayPositionBucket> result = schedulingService.getShiftsGroupedByDayAndPosition(7, startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).positions().size());
        assertEquals(2, result.get(1).positions().size());
        assertEquals(1, result.get(0).shiftCount());
        assertEquals(1, result.get(1).shiftCount());
        assertEquals(2.0f, result.get(0).totalDuration());
        assertEquals(6.0f, result.get(1).totalDuration());
        assertEquals("Bartender", result.get(0).positions().getFirst().position());
        assertEquals(1, result.get(0).positions().getFirst().shiftCount());
        assertEquals(2.0f, result.get(0).positions().getFirst().totalDuration());
        assertEquals(1, result.get(0).positions().getFirst().shifts().size());
        assertEquals(9003, result.get(0).positions().getFirst().shifts().getFirst().shiftId());
        assertEquals(LocalTime.of(22, 0), result.get(0).positions().getFirst().shifts().getFirst().startTime());
        assertEquals(LocalTime.MIDNIGHT, result.get(0).positions().getFirst().shifts().getFirst().endTime());
        assertEquals(1, result.get(1).positions().getFirst().shiftCount());
        assertEquals(6.0f, result.get(1).positions().getFirst().totalDuration());
        assertEquals(1, result.get(1).positions().getFirst().shifts().size());
        assertEquals(LocalTime.MIDNIGHT, result.get(1).positions().getFirst().shifts().getFirst().startTime());
        assertEquals(LocalTime.of(6, 0), result.get(1).positions().getFirst().shifts().getFirst().endTime());
        assertEquals("Server", result.get(0).positions().get(1).position());
        assertEquals(0, result.get(0).positions().get(1).shiftCount());
        assertEquals(0.0f, result.get(0).positions().get(1).totalDuration());
        assertTrue(result.get(0).positions().get(1).shifts().isEmpty());
        assertEquals("Server", result.get(1).positions().get(1).position());
        assertEquals(0, result.get(1).positions().get(1).shiftCount());
        assertEquals(0.0f, result.get(1).positions().get(1).totalDuration());
        assertTrue(result.get(1).positions().get(1).shifts().isEmpty());
    }

    @Test
    void dayPositionTimingGroupingGroupsShiftsByTimeWindow() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);

        when(positionService.getPositionsByCompanyId(7))
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
                                "amber"
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

        List<DayPositionTimingBucket> result = schedulingService.getShiftsGroupedByDayPositionAndTiming(7, startDate, endDate);

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

        when(categoryService.getCategoriesByCompanyId(7))
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
                                "amber"
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

        List<DayCategoryTimingBucket> result = schedulingService.getShiftsGroupedByDayCategoryAndTiming(7, startDate, endDate);

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

        when(categoryService.getCategoriesByCompanyId(7))
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
                                "amber"
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

        List<DayCategoryTimingBucket> result =
                schedulingService.getShiftsGroupedByDayCategoryShortNameAndTiming(7, startDate, endDate);

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

        when(positionService.getPositionsByCompanyId(7))
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
                                "amber"
                        )
                ));

        GroupedShiftsResponse result =
                schedulingService.getShiftsGrouped(7, startDate, endDate, ShiftGrouping.POSITION_SHIFT_TIMINGS);

        assertEquals(1, result.dates().size());
        assertEquals("Bartender", result.dates().getFirst().shiftGroups().getFirst().label());
        assertEquals(1, result.dates().getFirst().shiftGroups().getFirst().shiftGroups().size());
        assertEquals("9:00AM-5:00PM", result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().label());
        assertEquals(1, result.dates().getFirst().shiftGroups().getFirst().shiftGroups().getFirst().shifts().size());
    }

    @Test
    void getShiftsGrouped_returnsNormalizedCategoryShortNameResponse() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);

        when(categoryService.getCategoriesByCompanyId(7))
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
                                "amber"
                        )
                ));

        GroupedShiftsResponse result =
                schedulingService.getShiftsGrouped(7, startDate, endDate, ShiftGrouping.CAT_SHIFT_TIMINGS);

        ShiftGroup categoryGroup = result.dates().getFirst().shiftGroups().getFirst();
        assertEquals("FRT", categoryGroup.label());
        assertEquals(1, categoryGroup.shiftGroups().size());
        assertEquals("9:00AM-5:00PM", categoryGroup.shiftGroups().getFirst().label());
        assertEquals(1, categoryGroup.shiftGroups().getFirst().shifts().size());
    }

    @Test
    void dayShiftTimingGroupingGroupsShiftsByTimeWindowWithoutAggregates() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);

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
                                "amber"
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

        List<DayShiftTimingBucket> result = schedulingService.getShiftsGroupedByDayAndTiming(7, startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(startDate, result.get(0).date());
        assertEquals(1, result.get(0).shiftTimings().size());
        assertEquals("9:00AM-5:00PM", result.get(0).shiftTimings().getFirst().label());
        assertEquals(2, result.get(0).shiftTimings().getFirst().shifts().size());
        assertTrue(result.get(1).shiftTimings().isEmpty());
    }

    @Test
    void dayShiftTimingGroupingOrdersBucketsByTimeNotPosition() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);

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
                                "amber"
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

        List<DayShiftTimingBucket> result = schedulingService.getShiftsGroupedByDayAndTiming(7, startDate, endDate);

        assertEquals(2, result.getFirst().shiftTimings().size());
        assertEquals("9:00AM-5:00PM", result.getFirst().shiftTimings().get(0).label());
        assertEquals("10:00AM-6:00PM", result.getFirst().shiftTimings().get(1).label());
    }

    @Test
    void includesUnassignedShiftsInGroupedResults() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 25);

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

        List<EmployeeSchedule> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        assertEquals(1, result.size());
        EmployeeSchedule unassignedEntry = result.getFirst();
        assertEquals(null, unassignedEntry.getEmployeeId());
        assertEquals(1, unassignedEntry.getWeeklyShifts().get(0).shifts().size());
        assertEquals(9005, unassignedEntry.getWeeklyShifts().get(0).shifts().getFirst().shiftId());
    }

    private record TestProjection(
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
    ) implements EmployeeShiftProjection {
        @Override
        public Integer getShiftId() { return shiftId; }
        @Override
        public Integer getEmployeeId() { return employeeId; }
        @Override
        public String getFirstName() { return firstName; }
        @Override
        public String getLastName() { return lastName; }
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
        public String getPosition() { return position; }
        @Override
        public String getCategory() { return category; }
        @Override
        public String getDescription() { return description; }
        @Override
        public Float getDuration() { return duration; }
        @Override
        public Boolean getIsOvernight() { return isOvernight; }
        @Override
        public String getColor() { return color; }
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
            String color
    ) implements ShiftDetailsProjection {
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
        public String getColor() { return color; }
    }
}
