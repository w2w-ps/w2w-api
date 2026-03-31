package com.w2w.api.scheduling;

import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.dto.DayShiftBucketDto;
import com.w2w.api.scheduling.dto.EmployeeShiftProjection;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
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
    private SchedulingService schedulingService;

    @BeforeEach
    void setUp() {
        schedulingQueryRepository = Mockito.mock(SchedulingQueryRepository.class);
        shiftRepository = Mockito.mock(ShiftRepository.class);
        scheduleRepository = Mockito.mock(ScheduleRepository.class);
        schedulingService = new SchedulingService();
        ReflectionTestUtils.setField(schedulingService, "schedulingQueryRepository", schedulingQueryRepository);
        ReflectionTestUtils.setField(schedulingService, "shiftRepository", shiftRepository);
        ReflectionTestUtils.setField(schedulingService, "scheduleRepository", scheduleRepository);
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
        request.setColor("#FFAA00");

        Schedule schedule = new Schedule();
        schedule.setScheduleId(500);
        when(scheduleRepository.findByCompanyIdAndStartDate(1, date)).thenReturn(Optional.of(schedule));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift saved = schedulingService.saveShift(request);

        assertEquals(8.0f, saved.getDuration());
        assertEquals(false, saved.getIsOvernight());
        assertEquals(101, saved.getChangedBy());
        assertEquals(500, saved.getScheduleId());
        assertEquals("#FFAA00", saved.getColor());
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

        assertEquals(true, saved.getIsOvernight());
    }

    @Test
    void updateShiftUpdatesFieldsAndRecalculates() {
        Integer transactionId = 1001;
        UpdateShiftRequest request = new UpdateShiftRequest();
        request.setDescription("New description");
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(18, 0));
        request.setPosition(2);
        request.setColor("");

        Shift existingShift = new Shift();
        existingShift.setTransactionId(transactionId);
        existingShift.setEmployeeId(101);
        existingShift.setCompanyId(1);
        existingShift.setStartTime(LocalTime.of(9, 0));
        existingShift.setEndTime(LocalTime.of(17, 0));
        existingShift.setDuration(8.0f);

        when(shiftRepository.findById(transactionId)).thenReturn(Optional.of(existingShift));
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shift updated = schedulingService.updateShift(transactionId, request);

        assertEquals("New description", updated.getDescription());
        assertEquals(LocalTime.of(10, 0), updated.getStartTime());
        assertEquals(8.0f, updated.getDuration());
        assertEquals(2, updated.getRequiredSkillId());
        assertEquals("", updated.getColor());
    }

    @Test
    void softDeleteShiftSetsIsDeletedToTrue() {
        Integer transactionId = 1001;
        Shift existingShift = new Shift();
        existingShift.setTransactionId(transactionId);
        existingShift.setIsDeleted(false);

        when(shiftRepository.findById(transactionId)).thenReturn(Optional.of(existingShift));

        schedulingService.softDeleteShift(transactionId);

        assertEquals(true, existingShift.getIsDeleted());
        verify(shiftRepository).save(existingShift);
    }

    @Test
    void getShiftReturnsSingleQueryDetailsProjection() {
        Integer transactionId = 1001;
        when(shiftRepository.findShiftDetailsByTransactionId(transactionId))
                .thenReturn(Optional.of(new TestShiftDetailsProjection(
                        transactionId,
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
                        (short) 4
                )));

        ShiftResponseDto response = schedulingService.getShift(transactionId);

        assertEquals(transactionId, response.getTransactionId());
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
        assertEquals("4", response.getColor());
        verifyNoInteractions(scheduleRepository);
    }

    @Test
    void getShiftThrowsWhenProjectionNotFound() {
        Integer transactionId = 1001;
        when(shiftRepository.findShiftDetailsByTransactionId(transactionId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> schedulingService.getShift(transactionId)
        );

        assertEquals("Shift not found with id: 1001", exception.getMessage());
    }

    @Test
    void groupsShiftsRelativeToRequestedStartDateAndInitializesDayBuckets() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
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
                        8.0f
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
                        8.0f
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
        assertEquals(LocalTime.of(22, 0), secondBucket.getShifts().getFirst().getStartTime());
        assertEquals(LocalTime.MIDNIGHT, secondBucket.getShifts().getFirst().getEndTime());
        assertEquals(LocalTime.MIDNIGHT, thirdBucket.getShifts().getFirst().getStartTime());
        assertEquals(LocalTime.of(6, 0), thirdBucket.getShifts().getFirst().getEndTime());
        assertEquals(8.0, employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    @Test
    void includesOnlyInRangeSegmentForOvernightShiftAtRangeBoundary() {
        LocalDate startDate = LocalDate.of(2026, 3, 25);
        LocalDate endDate = LocalDate.of(2026, 3, 27);

        when(schedulingQueryRepository.findAllEmployeeShiftsInRange(7, startDate.minusDays(1), endDate))
                .thenReturn(List.of(new TestProjection(
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
                        4.0f
                )));

        List<EmployeeWithShiftsDto> result = schedulingService.getEmployeeShiftsGroupedInRange(7, startDate, endDate);

        EmployeeWithShiftsDto employee = result.getFirst();
        assertNotNull(employee.getWeeklyShifts().get(0));
        assertEquals(1, employee.getWeeklyShifts().get(0).getShifts().size());
        assertTrue(employee.getWeeklyShifts().get(1).getShifts().isEmpty());
        assertTrue(employee.getWeeklyShifts().get(2).getShifts().isEmpty());
        assertEquals(2.0, employee.getTotalHours());
        assertEquals(1, employee.getShiftCount());
    }

    private record TestProjection(
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
            Float duration
    ) implements EmployeeShiftProjection {
        @Override
        public Integer getEmployeeId() {
            return employeeId;
        }

        @Override
        public String getFirstName() {
            return firstName;
        }

        @Override
        public String getLastName() {
            return lastName;
        }

        @Override
        public List<String> getPhones() {
            return phones;
        }

        @Override
        public List<PositionDto> getAvailablePositions() {
            return availablePositions;
        }

        @Override
        public LocalDate getWeekCommencing() {
            return weekCommencing;
        }

        @Override
        public LocalTime getStartTime() {
            return startTime;
        }

        @Override
        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public String getPosition() {
            return position;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Float getDuration() {
            return duration;
        }

        @Override
        public Boolean getIsOvernight() {
            return isOvernight;
        }
    }

    private record TestShiftDetailsProjection(
            Integer transactionId,
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
        @Override
        public Integer getTransactionId() {
            return transactionId;
        }

        @Override
        public Integer getEmployeeId() {
            return employeeId;
        }

        @Override
        public Integer getCompanyId() {
            return companyId;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public LocalDate getDate() {
            return date;
        }

        @Override
        public LocalTime getStartTime() {
            return startTime;
        }

        @Override
        public LocalTime getEndTime() {
            return endTime;
        }

        @Override
        public Float getDuration() {
            return duration;
        }

        @Override
        public Boolean getIsOvernight() {
            return isOvernight;
        }

        @Override
        public String getPosition() {
            return position;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public Short getColor() {
            return color;
        }
    }
}
