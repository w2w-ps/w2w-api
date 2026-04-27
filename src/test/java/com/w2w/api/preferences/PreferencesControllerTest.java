package com.w2w.api.preferences;

import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.preferences.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreferencesController.class)
@AutoConfigureMockMvc(addFilters = false)
class PreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreferencesService preferencesService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getDayPreferences_returnsPreference() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 1);
        DayPreferenceResponse response = new DayPreferenceResponse(date, "G".repeat(96), true);

        when(preferencesService.getDayPreference(1, date)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/preferences/day")
                .param("employeeId", "1")
                .param("date", "2026-04-01")
                .param("companyId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefs").value("G".repeat(96)))
                .andExpect(jsonPath("$.isDayPrefs").value(true));
    }

    @Test
    void saveDayPreference_callsService() throws Exception {
        String request = """
                {
                  "employeeId": 1,
                  "companyId": 10,
                  "date": "2026-04-01",
                  "prefs": "G",
                  "compression": 0,
                  "editedBy": 1,
                  "isDayPrefs": true
                }
                """;

        mockMvc.perform(post("/api/preferences/day")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk());

        verify(preferencesService).saveDayPreference(any(DayPreferenceRequest.class));
    }

    @Test
    void getWeekPreferences_returnsPreference() throws Exception {
        LocalDate date = LocalDate.of(2026, 4, 1);
        WeekPreferenceResponse response = new WeekPreferenceResponse(LocalDate.of(2026, 3, 30), "W".repeat(672));

        when(preferencesService.getWeekPreference(1, date)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/preferences/week")
                .param("employeeId", "1")
                .param("startDate", "2026-04-01")
                .param("companyId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefs").value("W".repeat(672)));
    }

    @Test
    void getResolvedPreferences_returnsList() throws Exception {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 2);

        ResolvedPreferenceResponse res1 = new ResolvedPreferenceResponse(start, "G".repeat(96), "DAY", "Wednesday");
        ResolvedPreferenceResponse res2 = new ResolvedPreferenceResponse(end, "H".repeat(96), "HOUR", "Thursday");

        when(preferencesService.getResolvedPreferences(1, start, end)).thenReturn(List.of(res1, res2));

        mockMvc.perform(get("/api/preferences/resolved")
                .param("employeeId", "1")
                .param("startDate", "2026-04-01")
                .param("endDate", "2026-04-02")
                .param("companyId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].preferenceType").value("DAY"))
                .andExpect(jsonPath("$[1].preferenceType").value("HOUR"));
    }
}
