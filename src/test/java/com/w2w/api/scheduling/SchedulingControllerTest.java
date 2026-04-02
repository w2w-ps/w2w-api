package com.w2w.api.scheduling;

import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.scheduling.dto.ConflictDto;
import com.w2w.api.scheduling.dto.ConflictResponse;
import com.w2w.api.scheduling.dto.DayPositionBucketDto;
import com.w2w.api.scheduling.dto.EmployeeScheduledShiftDto;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.PositionShiftBucketDto;
import com.w2w.api.scheduling.dto.ShiftDto;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.model.Shift;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchedulingController.class)
@AutoConfigureMockMvc(addFilters = false)
class SchedulingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulingService schedulingService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getShift_returnsShiftDetails() throws Exception {
        when(schedulingService.getShift(9001)).thenReturn(createShiftResponse());

        mockMvc.perform(get("/api/scheduling/shifts/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftId").value(9001))
                .andExpect(jsonPath("$.employeeId").value(101))
                .andExpect(jsonPath("$.companyId").value(7))
                .andExpect(jsonPath("$.date").value("2026-03-25"))
                .andExpect(jsonPath("$.startTime").value("9:00AM"))
                .andExpect(jsonPath("$.endTime").value("5:00PM"))
                .andExpect(jsonPath("$.position").value("Bartender"))
                .andExpect(jsonPath("$.category").value("Front"))
                .andExpect(jsonPath("$.color").value("amber"));

        verify(schedulingService).getShift(9001);
    }

    @Test
    void createShift_returnsCreatedAndDelegatesRequest() throws Exception {
        String request = """
                {
                  "employeeId": 101,
                  "companyId": 7,
                  "description": "Opening shift",
                  "date": "2026-03-25",
                  "startTime": "9:00AM",
                  "endTime": "5:00PM",
                  "duration": 8.0,
                  "position": 12,
                  "category": 4,
                  "color": "amber"
                }
                """;

        mockMvc.perform(post("/api/scheduling/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        verify(schedulingService).saveShift(argThat(value ->
                value.getEmployeeId().equals(101)
                        && value.getCompanyId().equals(7)
                        && value.getDate().equals(LocalDate.of(2026, 3, 25))
                        && value.getStartTime().equals(LocalTime.of(9, 0))
                        && value.getEndTime().equals(LocalTime.of(17, 0))
                        && value.getPosition().equals(12)
                        && value.getCategory().equals(4)
                        && value.getColor().equals("amber")
        ));
    }

    @Test
    void updateShift_returnsUpdatedShift() throws Exception {
        when(schedulingService.updateShift(
                eq(9001),
                argThat(value -> value.shiftId().equals(9001)
                        && value.employeeId().equals(101)
                        && value.position().equals(12)
                        && value.color().equals("amber"))))
                .thenReturn(createShiftEntity());

        String request = """
                {
                  "shiftId": 9001,
                  "employeeId": 101,
                  "description": "Updated opening shift",
                  "startTime": "10:00AM",
                  "endTime": "6:00PM",
                  "position": 12,
                  "category": 4,
                  "color": "amber",
                  "date": "2026-03-25",
                  "duration": 8.0
                }
                """;

        mockMvc.perform(put("/api/scheduling/shifts/9001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftId").value(9001))
                .andExpect(jsonPath("$.employeeId").value(101))
                .andExpect(jsonPath("$.requiredSkillId").value(12))
                .andExpect(jsonPath("$.categoryId").value(4))
                .andExpect(jsonPath("$.startTime").value("10:00AM"))
                .andExpect(jsonPath("$.endTime").value("6:00PM"))
                .andExpect(jsonPath("$.color").value("amber"));
    }

    @Test
    void deleteShift_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/scheduling/shifts/9001"))
                .andExpect(status().isOk());

        verify(schedulingService).softDeleteShift(9001);
    }

    @Test
    void getShiftEmployees_returnsEmployeeBuckets() throws Exception {
        when(schedulingService.getEmployeeShiftsGroupedInRange(7, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26)))
                .thenReturn(List.of(createEmployeeWithShifts()));

        mockMvc.perform(get("/api/scheduling/shifts/employees")
                        .param("companyId", "7")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value(101))
                .andExpect(jsonPath("$[0].availablePositions", hasSize(1)))
                .andExpect(jsonPath("$[0].availablePositions[0].id").value(12))
                .andExpect(jsonPath("$[0].weeklyShifts['0'].date").value("2026-03-25"))
                .andExpect(jsonPath("$[0].weeklyShifts['0'].shifts", hasSize(1)))
                .andExpect(jsonPath("$[0].weeklyShifts['0'].shifts[0].startTime").value("9:00AM"))
                .andExpect(jsonPath("$[0].weeklyShifts['1'].date").value("2026-03-26"))
                .andExpect(jsonPath("$[0].weeklyShifts['1'].shifts", hasSize(0)));

        verify(schedulingService).getEmployeeShiftsGroupedInRange(7, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));
    }

    @Test
    void getShiftsGroupedInRange_deprecatedPathStillReturnsEmployeeBuckets() throws Exception {
        when(schedulingService.getEmployeeShiftsGroupedInRange(7, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26)))
                .thenReturn(List.of(createEmployeeWithShifts()));

        mockMvc.perform(get("/api/scheduling/employees")
                        .param("companyId", "7")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value(101));

        verify(schedulingService).getEmployeeShiftsGroupedInRange(7, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));
    }

    @Test
    void getShiftsGroupedByDayAndPosition_returnsGroupedBuckets() throws Exception {
        when(schedulingService.getShiftsGroupedByDayAndPosition(7, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26)))
                .thenReturn(List.of(
                        new DayPositionBucketDto(
                                LocalDate.of(2026, 3, 25),
                                List.of(
                                        new PositionShiftBucketDto(
                                                "Bartender",
                                                List.of(
                                                        new EmployeeScheduledShiftDto(
                                                                9001,
                                                                101,
                                                                "Ava",
                                                                "Stone",
                                                                List.of("111-222"),
                                                                LocalTime.of(9, 0),
                                                                LocalTime.of(17, 0),
                                                                "Front",
                                                                "Opening shift",
                                                                8.0f,
                                                                "amber"
                                                        )
                                                ),
                                                1,
                                                8.0f
                                        ),
                                        new PositionShiftBucketDto(
                                                "Server",
                                                List.of(),
                                                0,
                                                0.0f
                                        )
                                ),
                                1,
                                8.0f
                        ),
                        new DayPositionBucketDto(
                                LocalDate.of(2026, 3, 26),
                                List.of(
                                        new PositionShiftBucketDto("Bartender", List.of(), 0, 0.0f),
                                        new PositionShiftBucketDto("Server", List.of(), 0, 0.0f)
                                ),
                                0,
                                0.0f
                        )
                ));

        mockMvc.perform(get("/api/scheduling/shifts/day-position")
                        .param("companyId", "7")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-03-25"))
                .andExpect(jsonPath("$[0].positions", hasSize(2)))
                .andExpect(jsonPath("$[0].shiftCount").value(1))
                .andExpect(jsonPath("$[0].positions[0].position").value("Bartender"))
                .andExpect(jsonPath("$[0].positions[0].shifts", hasSize(1)))
                .andExpect(jsonPath("$[0].positions[0].shiftCount").value(1))
                .andExpect(jsonPath("$[0].positions[0].totalDuration").value(8.0))
                .andExpect(jsonPath("$[0].positions[1].position").value("Server"))
                .andExpect(jsonPath("$[0].positions[1].shiftCount").value(0))
                .andExpect(jsonPath("$[0].positions[1].shifts", hasSize(0)))
                .andExpect(jsonPath("$[0].positions[0].shifts[0].employeeId").value(101))
                .andExpect(jsonPath("$[0].positions[0].shifts[0].startTime").value("9:00AM"))
                .andExpect(jsonPath("$[0].totalDuration").value(8.0))
                .andExpect(jsonPath("$[1].shiftCount").value(0))
                .andExpect(jsonPath("$[1].totalDuration").value(0.0))
                .andExpect(jsonPath("$[1].positions", hasSize(2)))
                .andExpect(jsonPath("$[1].positions[0].position").value("Bartender"))
                .andExpect(jsonPath("$[1].positions[1].position").value("Server"));

        verify(schedulingService).getShiftsGroupedByDayAndPosition(7, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));
    }

    @Test
    void preCheck_withNoConflicts_returnsEmptyListAndHasConflictsFalse() throws Exception {
        when(schedulingService.validate(argThat(value -> value.operationType() == null && value.shift() == null)))
                .thenReturn(List.of());

        String request = """
                {
                  "operationType": null,
                  "shift": null
                }
                """;

        mockMvc.perform(post("/api/scheduling/validation/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConflicts").value(false))
                .andExpect(jsonPath("$.conflicts", hasSize(0)));
    }

    @Test
    void preCheck_withConflicts_returnsConflictPayload() throws Exception {
        when(schedulingService.validate(argThat(value ->
                value.operationType() != null && "Updated opening shift".equals(value.shift().description()))))
                .thenReturn(List.of(new ConflictDto("shift", "Overlaps existing shift")));

        String request = """
                {
                  "operationType": "UPDATE",
                  "shift": {
                    "shiftId": 9001,
                    "employeeId": 101,
                    "description": "Updated opening shift",
                    "startTime": "10:00AM",
                    "endTime": "6:00PM",
                    "position": 12,
                    "category": 4,
                    "color": "amber",
                    "date": "2026-03-25",
                    "duration": 8.0
                  }
                }
                """;

        mockMvc.perform(post("/api/scheduling/validation/precheck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConflicts").value(true))
                .andExpect(jsonPath("$.conflicts", hasSize(1)))
                .andExpect(jsonPath("$.conflicts[0].field").value("shift"))
                .andExpect(jsonPath("$.conflicts[0].message").value("Overlaps existing shift"));
    }

    @Test
    void getShiftsGroupedByDayAndPosition_missingCompanyId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/scheduling/shifts/day-position")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getShiftEmployees_invalidDate_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/scheduling/shifts/employees")
                        .param("companyId", "7")
                        .param("startDate", "25-03-2026")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateShift_acceptsExplicitDateAndTimeFormats() throws Exception {
        Shift updatedShift = createShiftEntity();
        updatedShift.setShiftId(1001);
        updatedShift.setRequiredSkillId(2);

        when(schedulingService.updateShift(
                eq(1001),
                argThat(request ->
                        request.shiftId().equals(1001)
                                && request.employeeId().equals(101)
                                && request.date().equals(LocalDate.of(2026, 3, 31))
                                && request.startTime().equals(LocalTime.of(10, 0))
                                && request.endTime().equals(LocalTime.of(18, 0))
                                && request.position().equals(2)
                                && request.category().equals(4)
                                && request.color().equals("amber")
                )
        )).thenReturn(updatedShift);

        String request = """
                {
                  "shiftId": 1001,
                  "employeeId": 101,
                  "description": "Updated opening shift",
                  "date": "2026-03-31",
                  "startTime": "10:00AM",
                  "endTime": "6:00PM",
                  "position": 2,
                  "category": 4,
                  "color": "amber"
                }
                """;

        mockMvc.perform(put("/api/scheduling/shifts/1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftId", is(1001)))
                .andExpect(jsonPath("$.employeeId", is(101)))
                .andExpect(jsonPath("$.requiredSkillId", is(2)))
                .andExpect(jsonPath("$.categoryId", is(4)))
                .andExpect(jsonPath("$.startTime", is("10:00AM")))
                .andExpect(jsonPath("$.endTime", is("6:00PM")))
                .andExpect(jsonPath("$.color", is("amber")));
    }

    @Test
    void updateShift_rejectsInvalidDateFormat() throws Exception {
        String request = """
                {
                  "shiftId": 1001,
                  "employeeId": 101,
                  "description": "Updated opening shift",
                  "date": "31-03-2026",
                  "startTime": "10:00AM",
                  "endTime": "6:00PM",
                  "position": 2
                }
                """;

        mockMvc.perform(put("/api/scheduling/shifts/1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateShift_rejectsInvalidTimeFormat() throws Exception {
        String request = """
                {
                  "shiftId": 1001,
                  "employeeId": 101,
                  "description": "Updated opening shift",
                  "date": "2026-03-31",
                  "startTime": "10:00:00",
                  "endTime": "18:00:00",
                  "position": 2
                }
                """;

        mockMvc.perform(put("/api/scheduling/shifts/1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest());
    }

    private ShiftResponseDto createShiftResponse() {
        ShiftResponseDto dto = new ShiftResponseDto();
        dto.setShiftId(9001);
        dto.setEmployeeId(101);
        dto.setCompanyId(7);
        dto.setDescription("Opening shift");
        dto.setDate(LocalDate.of(2026, 3, 25));
        dto.setStartTime(LocalTime.of(9, 0));
        dto.setEndTime(LocalTime.of(17, 0));
        dto.setDuration(8.0f);
        dto.setIsOvernight(false);
        dto.setPosition("Bartender");
        dto.setCategory("Front");
        dto.setColor("amber");
        return dto;
    }

    private Shift createShiftEntity() {
        Shift shift = new Shift();
        shift.setShiftId(9001);
        shift.setEmployeeId(101);
        shift.setScheduleId(500);
        shift.setCompanyId(7);
        shift.setDescription("Updated opening shift");
        shift.setStartTime(LocalTime.of(10, 0));
        shift.setEndTime(LocalTime.of(18, 0));
        shift.setDuration(8.0f);
        shift.setIsOvernight(false);
        shift.setRequiredSkillId(12);
        shift.setCategoryId(4);
        shift.setColor("amber");
        shift.setIsDeleted(false);
        shift.setChangedBy(101);
        return shift;
    }

    private EmployeeWithShiftsDto createEmployeeWithShifts() {
        EmployeeWithShiftsDto employee = new EmployeeWithShiftsDto(
                101,
                "Ava",
                "Stone",
                List.of("111-222"),
                List.of(new PositionDto(12, "Bartender")),
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );
        employee.addShiftToDay(0, LocalDate.of(2026, 3, 25), new ShiftDto(
                9001,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                "Bartender",
                "Front",
                "Opening shift",
                8.0f,
                "amber"
        ));
        employee.setTotalHours(8.0);
        employee.setShiftCount(1);
        return employee;
    }
}
