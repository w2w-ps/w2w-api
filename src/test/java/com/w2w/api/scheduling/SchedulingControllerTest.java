package com.w2w.api.scheduling;

import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.scheduling.dto.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

        verify(schedulingService).saveShift(101, "Opening shift", LocalDate.of(2026, 3, 25),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 8.0f, 12, 4, "amber");
    }

    @Test
    void updateShift_returnsUpdatedShift() throws Exception {
        ShiftResponse response = new ShiftResponse(
                9001,
                101,
                7,
                "Opening shift",
                LocalDate.of(2026, 3, 25),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                8.0f,
                false,
                "Bartender",
                "Front",
                "amber"
        );

        when(schedulingService.updateShift(
                eq(9001),
                argThat(value -> value.shiftId().equals(9001)
                        && value.employeeId().equals(101)
                        && value.position().equals(12)
                        && value.color().equals("amber"))))
                .thenReturn(response);

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
                .andExpect(jsonPath("$.position").value("Bartender"))
                .andExpect(jsonPath("$.category").value("Front"))
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
        when(schedulingService.getEmployeeShiftsGroupedInRange(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26)))
                .thenReturn(List.of(createEmployeeWithShifts()));

        mockMvc.perform(get("/api/scheduling/shifts/employees")
                        .param("companyId", "7")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value(101))
                .andExpect(jsonPath("$[0].availablePositions", hasSize(1)))
                .andExpect(jsonPath("$[0].availablePositions[0].positionId").value(12))
                .andExpect(jsonPath("$[0].weeklyShifts['0'].date").value("2026-03-25"))
                .andExpect(jsonPath("$[0].weeklyShifts['0'].shifts", hasSize(1)))
                .andExpect(jsonPath("$[0].weeklyShifts['0'].shifts[0].startTime").value("9:00AM"))
                .andExpect(jsonPath("$[0].weeklyShifts['1'].date").value("2026-03-26"))
                .andExpect(jsonPath("$[0].weeklyShifts['1'].shifts", hasSize(0)));

        verify(schedulingService).getEmployeeShiftsGroupedInRange(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));
    }

    @Test
    void getShiftsGroupedInRange_deprecatedPathStillReturnsEmployeeBuckets() throws Exception {
        when(schedulingService.getEmployeeShiftsGroupedInRange(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26)))
                .thenReturn(List.of(createEmployeeWithShifts()));

        mockMvc.perform(get("/api/scheduling/employees")
                        .param("companyId", "7")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].employeeId").value(101));

        verify(schedulingService).getEmployeeShiftsGroupedInRange(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));
    }

    @Test
    void getShiftsGroupedByDateAndPosition_returnsGroupedBuckets() throws Exception {
        when(schedulingService.getShiftsGroupedByDateAndPosition(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26)))
                .thenReturn(List.of(
                        new DayPositionBucket(
                                LocalDate.of(2026, 3, 25),
                                List.of(
                                        new PositionShiftBucket(
                                                "Bartender",
                                                new ArrayList<>(List.of(
                                                        new EmployeeShift(
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
                                                )),
                                                1,
                                                8.0f
                                        ),
                                        new PositionShiftBucket(
                                                "Server",
                                                new ArrayList<>(),
                                                0,
                                                0.0f
                                        )
                                ),
                                1,
                                8.0f
                        ),
                        new DayPositionBucket(
                                LocalDate.of(2026, 3, 26),
                                List.of(
                                        new PositionShiftBucket("Bartender", new ArrayList<>(), 0, 0.0f),
                                        new PositionShiftBucket("Server", new ArrayList<>(), 0, 0.0f)
                                ),
                                0,
                                0.0f
                        )
                ));

        mockMvc.perform(get("/api/scheduling/shifts/date-position")
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

        verify(schedulingService).getShiftsGroupedByDateAndPosition(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));
    }

    @Test
    void getGroupedShifts_positionShiftTimings_returnsNormalizedNestedGroups() throws Exception {
        when(schedulingService.getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.POSITION_SHIFT_TIMINGS
        )).thenReturn(new GroupedShiftsResponse(List.of(
                new GroupedShiftDate(
                        LocalDate.of(2026, 3, 25),
                        List.of(new ShiftGroup(
                                "Bartender",
                                List.of(new ShiftGroup(
                                        "9:00AM-5:00PM",
                                        List.of(),
                                        List.of()
                                )),
                                List.of()
                        ))
                )
        )));

        mockMvc.perform(get("/api/scheduling/shifts/grouped")
                        .param("companyId", "7")
                        .param("grouping", "position_shift_timings")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].label").value("Bartender"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].label").value("9:00AM-5:00PM"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].shifts", hasSize(0)));

        verify(schedulingService).getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.POSITION_SHIFT_TIMINGS
        );
    }

    @Test
    void getGroupedShifts_categoryShiftTimings_returnsNormalizedCategoryGroups() throws Exception {
        when(schedulingService.getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.CATEGORY_SHIFT_TIMINGS
        )).thenReturn(new GroupedShiftsResponse(List.of(
                new GroupedShiftDate(
                        LocalDate.of(2026, 3, 25),
                        List.of(new ShiftGroup(
                                "Front",
                                List.of(new ShiftGroup(
                                        "9:00AM-5:00PM",
                                        List.of(),
                                        List.of()
                                )),
                                List.of()
                        ))
                )
        )));

        mockMvc.perform(get("/api/scheduling/shifts/grouped")
                        .param("companyId", "7")
                        .param("grouping", "category_shift_timings")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].label").value("Front"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].label").value("9:00AM-5:00PM"));

        verify(schedulingService).getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.CATEGORY_SHIFT_TIMINGS
        );
    }

    @Test
    void getGroupedShifts_catShiftTimings_returnsNormalizedShortCategoryGroups() throws Exception {
        when(schedulingService.getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.CAT_SHIFT_TIMINGS
        )).thenReturn(new GroupedShiftsResponse(List.of(
                new GroupedShiftDate(
                        LocalDate.of(2026, 3, 25),
                        List.of(new ShiftGroup(
                                "FRT",
                                List.of(new ShiftGroup(
                                        "9:00AM-5:00PM",
                                        List.of(),
                                        List.of()
                                )),
                                List.of()
                        ))
                )
        )));

        mockMvc.perform(get("/api/scheduling/shifts/grouped")
                        .param("companyId", "7")
                        .param("grouping", "cat_shift_timings")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].label").value("FRT"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shiftGroups[0].label").value("9:00AM-5:00PM"));

        verify(schedulingService).getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.CAT_SHIFT_TIMINGS
        );
    }

    @Test
    void getGroupedShifts_shiftTimings_returnsNormalizedTimingGroups() throws Exception {
        when(schedulingService.getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.SHIFT_TIMINGS
        )).thenReturn(new GroupedShiftsResponse(List.of(
                new GroupedShiftDate(
                        LocalDate.of(2026, 3, 25),
                        List.of(new ShiftGroup("9:00AM-5:00PM", List.of(), List.of()))
                )
        )));

        mockMvc.perform(get("/api/scheduling/shifts/grouped")
                        .param("companyId", "7")
                        .param("grouping", "shift_timings")
                        .param("startDate", "2026-03-25")
                        .param("endDate", "2026-03-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dates", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].date").value("2026-03-25"))
                .andExpect(jsonPath("$.dates[0].shiftGroups", hasSize(1)))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].label").value("9:00AM-5:00PM"))
                .andExpect(jsonPath("$.dates[0].shiftGroups[0].shifts", hasSize(0)));

        verify(schedulingService).getShiftsGrouped(
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26),
                ShiftGrouping.SHIFT_TIMINGS
        );
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
                .thenReturn(List.of(new ConflictItem("shift", "Overlaps existing shift")));

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
    void getShiftsGroupedByDateAndPosition_missingCompanyId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/scheduling/shifts/date-position")
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
        ShiftResponse response = new ShiftResponse(
                1001,
                101,
                7,
                "Opening shift",
                LocalDate.of(2026, 3, 25),
                LocalTime.of(10, 0),
                LocalTime.of(18, 0),
                8.0f,
                false,
                "Bartender",
                "Front",
                "amber"
        );

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
        )).thenReturn(response);

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
                .andExpect(jsonPath("$.position", is("Bartender")))
                .andExpect(jsonPath("$.category", is("Front")))
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

    private ShiftResponse createShiftResponse() {
        return new ShiftResponse(
                9001,
                101,
                7,
                "Opening shift",
                LocalDate.of(2026, 3, 25),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                8.0f,
                false,
                "Bartender",
                "Front",
                "amber"
        );
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
        shift.setRequiredPositionId(12);
        shift.setCategoryId(4);
        shift.setColor("amber");
        shift.setIsDeleted(false);
        shift.setChangedBy(101);
        return shift;
    }

    private EmployeeSchedule createEmployeeWithShifts() {
        EmployeeSchedule employee = new EmployeeSchedule(
                101,
                "Ava",
                "Stone",
                List.of("111-222"),
                List.of(new PositionSummary(12, "Bartender")),
                LocalDate.of(2026, 3, 25),
                LocalDate.of(2026, 3, 26)
        );
        employee.addShiftToDay(0, LocalDate.of(2026, 3, 25), new ShiftSummary(
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
