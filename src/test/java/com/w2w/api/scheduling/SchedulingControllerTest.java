package com.w2w.api.scheduling;

import com.w2w.api.login.JwtUtil;
import com.w2w.api.scheduling.model.Shift;
import com.w2w.api.scheduling.dto.ConflictDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchedulingController.class)
class SchedulingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulingService schedulingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void preCheck_withNoConflicts_returnsEmptyListAndHasConflictsFalse() throws Exception {
        List<ConflictDto> conflicts = new ArrayList<>();
        String request = """
                {
                  "operationType": "CREATE",
                  "shiftData": {
                    "shiftId": null,
                    "employeeId": 1,
                    "description": null,
                    "startTime": "09:00:00",
                    "endTime": "17:00:00",
                    "position": null,
                    "category": null,
                    "color": null,
                    "date": null,
                    "duration": null
                  }
                }
                """;

        when(schedulingService.validate(any())).thenReturn(conflicts);

        mockMvc.perform(post("/api/scheduling/validation/precheck")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConflicts", is(false)))
                .andExpect(jsonPath("$.conflicts", hasSize(0)));
    }

    @Test
    void updateShift_acceptsExplicitDateAndTimeFormats() throws Exception {
        Shift updatedShift = new Shift();
        updatedShift.setShiftId(1001);
        updatedShift.setEmployeeId(101);
        updatedShift.setCompanyId(7);
        updatedShift.setDescription("Updated opening shift");
        updatedShift.setStartTime(LocalTime.of(10, 0));
        updatedShift.setEndTime(LocalTime.of(18, 0));
        updatedShift.setRequiredSkillId(2);
        updatedShift.setCategoryId(4);
        updatedShift.setColor("amber");

        when(schedulingService.updateShift(
                org.mockito.ArgumentMatchers.eq(1001),
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
}
