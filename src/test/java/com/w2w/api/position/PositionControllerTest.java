package com.w2w.api.position;

import com.w2w.api.config.GlobalExceptionHandler;
import com.w2w.api.config.exception.ForbiddenOperationException;
import com.w2w.api.config.exception.ResourceNotFoundException;
import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.position.dto.PositionSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PositionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PositionService positionService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getPositions_withoutStatus_returnsActivePositions() throws Exception {
        when(positionService.get("active"))
                .thenReturn(List.of(
                        new PositionSummary(101, "Bartender"),
                        new PositionSummary(102, "Server")
                ));

        mockMvc.perform(get("/api/positions").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].positionId").value(101))
                .andExpect(jsonPath("$.positions[0].description").value("Bartender"))
                .andExpect(jsonPath("$.positions[1].positionId").value(102))
                .andExpect(jsonPath("$.positions[1].description").value("Server"));

        verify(positionService).get("active");
    }

    @Test
    void getPositions_withStatusFilter_returnsPositions() throws Exception {
        when(positionService.get("inactive"))
                .thenReturn(List.of(new PositionSummary(103, "Archived Server")));

        mockMvc.perform(get("/api/positions").param("companyId", "1").param("status", "inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].positionId").value(103))
                .andExpect(jsonPath("$.positions[0].description").value("Archived Server"));

        verify(positionService).get("inactive");
    }

    @Test
    void getPositionById_returnsPositionWhenFound() throws Exception {
        when(positionService.getPositionSummaryById(101))
                .thenReturn(Optional.of(new PositionSummary(101, "Bartender")));

        mockMvc.perform(get("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId").value(101))
                .andExpect(jsonPath("$.description").value("Bartender"));

        verify(positionService).getPositionSummaryById(101);
    }

    @Test
    void getPositionById_returnsNotFoundWhenMissing() throws Exception {
        when(positionService.getPositionSummaryById(101)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isNotFound());

        verify(positionService).getPositionSummaryById(101);
    }

    @Test
    void createPosition_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 1,
                                  "description": "Host"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(positionService).create("Host");
    }

    @Test
    void createPosition_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage positions."))
                .when(positionService)
                .create("Host");

        mockMvc.perform(post("/api/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 1,
                                  "description": "Host"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePosition_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/positions/101")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Lead Bartender"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(positionService).update(eq(101), argThat(value ->
                value.description().equals("Lead Bartender")));
    }

    @Test
    void updatePosition_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResourceNotFoundException("Position not found"))
                .when(positionService)
                .update(eq(101), argThat(value -> value.description().equals("Lead Bartender")));

        mockMvc.perform(put("/api/positions/101")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Lead Bartender"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePosition_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage positions."))
                .when(positionService)
                .update(eq(101), argThat(value -> value.description().equals("Lead Bartender")));

        mockMvc.perform(put("/api/positions/101")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Lead Bartender"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePosition_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isNoContent());

        verify(positionService).delete(101);
    }

    @Test
    void deletePosition_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResourceNotFoundException("Position not found"))
                .when(positionService)
                .delete(101);

        mockMvc.perform(delete("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePosition_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage positions."))
                .when(positionService)
                .delete(101);

        mockMvc.perform(delete("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void restorePosition_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/positions/101/restore").param("companyId", "1"))
                .andExpect(status().isNoContent());

        verify(positionService).restore(101);
    }

    @Test
    void restorePosition_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResourceNotFoundException("Position not found"))
                .when(positionService)
                .restore(101);

        mockMvc.perform(post("/api/positions/101/restore").param("companyId", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void restorePosition_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage positions."))
                .when(positionService)
                .restore(101);

        mockMvc.perform(post("/api/positions/101/restore").param("companyId", "1"))
                .andExpect(status().isForbidden());
    }
}
