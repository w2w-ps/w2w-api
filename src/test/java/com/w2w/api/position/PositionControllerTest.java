package com.w2w.api.position;

import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.position.dto.PositionSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PositionController.class)
@AutoConfigureMockMvc(addFilters = false)
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
    void getAllPositions_returnsPositions() throws Exception {
        when(positionService.getPositions(1, "all"))
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

        verify(positionService).getPositions(1, "all");
    }

    @Test
    void getPositions_withStatusFilter_returnsPositions() throws Exception {
        when(positionService.getPositions(1, "inactive"))
                .thenReturn(List.of(new PositionSummary(103, "Archived Server")));

        mockMvc.perform(get("/api/positions").param("companyId", "1").param("status", "inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].positionId").value(103))
                .andExpect(jsonPath("$.positions[0].description").value("Archived Server"));

        verify(positionService).getPositions(1, "inactive");
    }

    @Test
    void getActivePositions_returnsPositions() throws Exception {
        when(positionService.getPositions(1, "active"))
                .thenReturn(List.of(new PositionSummary(101, "Bartender")));

        mockMvc.perform(get("/api/positions/active").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].positionId").value(101))
                .andExpect(jsonPath("$.positions[0].description").value("Bartender"));

        verify(positionService).getPositions(1, "active");
    }

    @Test
    void getNonActivePositions_returnsPositions() throws Exception {
        when(positionService.getPositions(1, "inactive"))
                .thenReturn(List.of(new PositionSummary(103, "Archived Server")));

        mockMvc.perform(get("/api/positions/non-active").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].positionId").value(103))
                .andExpect(jsonPath("$.positions[0].description").value("Archived Server"));

        verify(positionService).getPositions(1, "inactive");
    }

    @Test
    void getPositionById_returnsPositionWhenFound() throws Exception {
        when(positionService.getPositionById(101, 1))
                .thenReturn(Optional.of(new PositionSummary(101, "Bartender")));

        mockMvc.perform(get("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId").value(101))
                .andExpect(jsonPath("$.description").value("Bartender"));

        verify(positionService).getPositionById(101, 1);
    }

    @Test
    void getPositionById_returnsNotFoundWhenMissing() throws Exception {
        when(positionService.getPositionById(101, 1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isNotFound());

        verify(positionService).getPositionById(101, 1);
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

        verify(positionService).createPosition(argThat(value ->
                value.companyId().equals(1) && value.description().equals("Host")));
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

        verify(positionService).updatePosition(eq(101), eq(1), argThat(value ->
                value.description().equals("Lead Bartender")));
    }

    @Test
    void updatePosition_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Position not found"))
                .when(positionService)
                .updatePosition(eq(101), eq(1), argThat(value -> value.description().equals("Lead Bartender")));

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
    void deletePosition_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isNoContent());

        verify(positionService).deletePosition(101, 1);
    }

    @Test
    void deletePosition_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Position not found"))
                .when(positionService)
                .deletePosition(101, 1);

        mockMvc.perform(delete("/api/positions/101").param("companyId", "1"))
                .andExpect(status().isNotFound());
    }
}
