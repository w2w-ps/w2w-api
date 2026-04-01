package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ConflictDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchedulingController.class)
class SchedulingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SchedulingService schedulingService;

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
}
