package com.w2w.api.scheduling;

import com.w2w.api.position.PositionService;
import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.DayPositionBucketDto;
import com.w2w.api.scheduling.dto.DayShiftBucketDto;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.PositionShiftBucketDto;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.ShiftDto;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Schedule;
import com.w2w.api.scheduling.model.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private SchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        schedulingQueryRepository = Mockito.mock(SchedulingQueryRepository.class);
        shiftRepository = Mockito.mock(ShiftRepository.class);
        scheduleRepository = Mockito.mock(ScheduleRepository.class);
        positionService = Mockito.mock(PositionService.class);
        schedulingService = new SchedulingService();
        ReflectionTestUtils.setField(schedulingService, "schedulingQueryRepository", schedulingQueryRepository);
        ReflectionTestUtils.setField(schedulingService, "shiftRepository", shiftRepository);
        ReflectionTestUtils.setField(schedulingService, "scheduleRepository", scheduleRepository);
        ReflectionTestUtils.setField(schedulingService, "positionService", positionService);
    }

    @Test
    void saveShiftCalculatesDurationAndOvernightStatus() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest();
        request.setEmployeeId(101);
        request.setCompanyId(1);
        request.setDate(date);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setPosition(1);
        request.setColor("amber");

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
        CreateShiftRequest request = new CreateShiftRequest();
        request.setEmployeeId(101);
        request.setCompanyId(1);
        request.setDate(date);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setPosition(1);
        request.setColor("");

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
        CreateShiftRequest request = new CreateShiftRequest();
        request.setEmployeeId(101);
        request.setCompanyId(1);
        request.setDate(date);
        request.setStartTime(LocalTime.of(22, 0));
        request.setEndTime(LocalTime.of(6, 0));
        request.setPosition(1);

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
        CreateShiftRequest request = new CreateShiftRequest();
        request.setEmployeeId(101);
        request.setCompanyId(1);
        request.setDate(date);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setPosition(1);

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
        CreateShiftRequest request = new CreateShiftRequest();
        request.setEmployeeId(101);
        request.setCompanyId(1);
        request.setDate(date);
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setPosition(1);

        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setScheduleId(600);
            return schedule;
        });
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        schedulingService.saveShift(request);

        org.mockito.ArgumentCaptor<Schedule> scheduleCaptor = org.mockito.ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertEquals(false, scheduleCaptor.getValue().isPublished());
    }

    @Test
    void saveShiftAlwaysDerivesOvernightFromTimes() {
        LocalDate date = LocalDate.of(2026, 3, 31);
        CreateShiftRequest request = new CreateShiftRequest();
        request.setEmployeeId(101);
        request.setCompanyId(1);
        request.setDate(date);
        request.setStartTime(LocalTime.of(22, 0));
        request.setEndTime(LocalTime.of(6, 0));
        request.setPosition(1);

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

        ShiftResponseDto updated = schedulingService.updateShift(shiftId, request);

        assertEquals("New description", updated.getDescription());
        assertEquals(LocalTime.of(10, 0), updated.getStartTime());
        assertEquals(8.0f, updated.getDuration());
        assertEquals("Bartender", updated.getPosition());
        assertEquals("amber", updated.getColor());
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

        ShiftResponseDto response = schedulingService.getShift(shiftId);

        assertEquals(shiftId, response.getShiftId());
        assertEquals(101, response.getEmployeeId());
        assertEquals(7, response.getCompanyId());
        assertEquals("Opening shift", response.getDescription());
        assertEquals(LocalDate.of(2026, 3, 31), response.getDate());
        assertEquals(LocalTime.of(9, 0), response.getStartTime());
        assertEquals(LocalTime.of(17, 0), response.getEndTime());
        assertEquals(8.0f, response.getDuration());
        assertEquals(false, response.getIsOvernight());
        assertEquals("Bartender", response.getPosition());
        assertEquals("Front", response.getCategory());
        assertEquals("", response.getColor());
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
                        List.of(new PositionDto(12, "Bartender"), new PositionDto(19, "Server")),
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

        List<EmployeeWithShiftsDto> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        assertEquals(1, result.size());
        EmployeeWithShiftsDto employee = result.getFirst();
        assertEquals(2, employee.getAvailablePositions().size());
        assertEquals(12, employee.getAvailablePositions().getFirst().getId());
        assertEquals("Bartender", employee.getAvailablePositions().getFirst().getName());

        assertEquals(3, employee.getWeeklyShifts().size());
        assertEquals(startDate, employee.getWeeklyShifts().get(0).getDate());
        assertEquals(startDate.plusDays(1), employee.getWeeklyShifts().get(1).getDate());
        assertEquals(startDate.plusDays(2), employee.getWeeklyShifts().get(2).getDate());

        DayShiftBucketDto firstBucket = employee.getWeeklyShifts().get(0);
        assertEquals(1, firstBucket.getShifts().size());
        assertEquals(9001, firstBucket.getShifts().getFirst().getShiftId());
        assertEquals("amber", firstBucket.getShifts().getFirst().getColor());
        assertTrue(employee.getWeeklyShifts().get(1).getShifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).getShifts().isEmpty());
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
                        List.of(new PositionDto(12, "Bartender")),
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

        List<EmployeeWithShiftsDto> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        EmployeeWithShiftsDto employee = result.getFirst();
        assertEquals(3, employee.getWeeklyShifts().size());

        DayShiftBucketDto secondBucket = employee.getWeeklyShifts().get(1);
        DayShiftBucketDto thirdBucket = employee.getWeeklyShifts().get(2);

        assertEquals(LocalDate.of(2026, 3, 26), secondBucket.getDate());
        assertEquals(LocalDate.of(2026, 3, 27), thirdBucket.getDate());
        assertEquals(1, secondBucket.getShifts().size());
        assertEquals(1, thirdBucket.getShifts().size());
        assertEquals(9002, secondBucket.getShifts().getFirst().getShiftId());
        assertEquals("blue", secondBucket.getShifts().getFirst().getColor());
        assertTrue(employee.getWeeklyShifts().get(1).getShifts().getFirst().getColor().length() > 0);
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

        List<EmployeeWithShiftsDto> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        EmployeeWithShiftsDto employee = result.getFirst();
        assertNotNull(employee.getWeeklyShifts().get(0));
        assertEquals(1, employee.getWeeklyShifts().get(0).getShifts().size());
        assertEquals(9003, employee.getWeeklyShifts().get(0).getShifts().getFirst().getShiftId());
        assertEquals("charcoal", employee.getWeeklyShifts().get(0).getShifts().getFirst().getColor());
        assertTrue(employee.getWeeklyShifts().get(0).getShifts().getFirst().getColor().length() > 0);
        assertTrue(employee.getWeeklyShifts().get(1).getShifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).getShifts().isEmpty());
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

        List<EmployeeWithShiftsDto> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        ShiftDto groupedShift = result.getFirst().getWeeklyShifts().get(0).getShifts().getFirst();
        assertEquals(null, groupedShift.getShiftId());
        assertEquals(null, groupedShift.getColor());
    }

    @Test
    void groupsShiftsByDayAndPositionAndInitializesEmptyDayBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 26);

        when(positionService.getPositionsByCompanyId(7))
                .thenReturn(List.of(
                        new PositionDto(12, "Bartender"),
                        new PositionDto(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(
                        new TestProjection(
                                9001,
                                101,
                                "Ava",
                                "Stone",
                                List.of("111-222"),
                                List.of(new PositionDto(12, "Bartender")),
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
                                List.of(new PositionDto(19, "Server")),
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

        List<DayPositionBucketDto> result = schedulingService.getShiftsGroupedByDayAndPosition(7, startDate, endDate);

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

        List<PositionShiftBucketDto> firstDayPositions = result.get(0).positions();
        assertEquals(2, firstDayPositions.size());
        assertEquals("Bartender", firstDayPositions.get(0).position());
        assertEquals(1, firstDayPositions.get(0).shiftCount());
        assertEquals(8.0f, firstDayPositions.get(0).totalDuration());
        assertEquals("Server", firstDayPositions.get(1).position());
        assertEquals(1, firstDayPositions.get(1).shiftCount());
        assertEquals(8.0f, firstDayPositions.get(1).totalDuration());
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
                        new PositionDto(12, "Bartender"),
                        new PositionDto(19, "Server")
                ));
        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
                        9003,
                        101,
                        "Ava",
                        "Stone",
                        List.of("111-222"),
                        List.of(new PositionDto(12, "Bartender")),
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

        List<DayPositionBucketDto> result = schedulingService.getShiftsGroupedByDayAndPosition(7, startDate, endDate);

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
        assertEquals(9003, result.get(0).positions().getFirst().shifts().getFirst().shiftId());
        assertEquals(LocalTime.of(22, 0), result.get(0).positions().getFirst().shifts().getFirst().startTime());
        assertEquals(LocalTime.MIDNIGHT, result.get(0).positions().getFirst().shifts().getFirst().endTime());
        assertEquals(1, result.get(1).positions().getFirst().shiftCount());
        assertEquals(6.0f, result.get(1).positions().getFirst().totalDuration());
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

    private record TestProjection(
            Integer shiftId,
            Integer employeeId,
            String firstName,
            String lastName,
            List<String> phones,
            List<PositionDto> availablePositions,
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
        public List<PositionDto> getAvailablePositions() { return availablePositions; }
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
