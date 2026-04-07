package com.w2w.api.positiongroup;

import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.positiongroup.dto.PositionGroupSummary;
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
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PositionGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class PositionGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PositionGroupService positionGroupService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getPositionGroups_returnsGroups() throws Exception {
        when(positionGroupService.getPositionGroups("all"))
                .thenReturn(List.of(
                        new PositionGroupSummary(
                                201,
                                "Front of House",
                                List.of(new PositionSummary(101, "Server"))
                        )
                ));

        mockMvc.perform(get("/api/position-groups").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionGroups[0].id").value(201))
                .andExpect(jsonPath("$.positionGroups[0].name").value("Front of House"))
                .andExpect(jsonPath("$.positionGroups[0].positions[0].positionId").value(101))
                .andExpect(jsonPath("$.positionGroups[0].positions[0].description").value("Server"));

        verify(positionGroupService).getPositionGroups("all");
    }

    @Test
    void getPositionGroups_withStatusFilter_returnsGroups() throws Exception {
        when(positionGroupService.getPositionGroups("inactive"))
                .thenReturn(List.of(
                        new PositionGroupSummary(
                                202,
                                "Archived Group",
                                List.of(new PositionSummary(102, "Bartender"))
                        )
                ));

        mockMvc.perform(get("/api/position-groups").param("companyId", "1").param("status", "inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionGroups[0].id").value(202))
                .andExpect(jsonPath("$.positionGroups[0].name").value("Archived Group"));

        verify(positionGroupService).getPositionGroups("inactive");
    }

    @Test
    void getActivePositionGroups_returnsGroups() throws Exception {
        when(positionGroupService.getPositionGroups("active"))
                .thenReturn(List.of(
                        new PositionGroupSummary(
                                201,
                                "Front of House",
                                List.of(new PositionSummary(101, "Server"))
                        )
                ));

        mockMvc.perform(get("/api/position-groups/active").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionGroups[0].id").value(201));

        verify(positionGroupService).getPositionGroups("active");
    }

    @Test
    void getInactivePositionGroups_returnsGroups() throws Exception {
        when(positionGroupService.getPositionGroups("inactive"))
                .thenReturn(List.of(
                        new PositionGroupSummary(
                                202,
                                "Archived Group",
                                List.of(new PositionSummary(102, "Bartender"))
                        )
                ));

        mockMvc.perform(get("/api/position-groups/non-active").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionGroups[0].id").value(202));

        verify(positionGroupService).getPositionGroups("inactive");
    }

    @Test
    void getPositionGroupById_returnsGroupWhenFound() throws Exception {
        when(positionGroupService.getPositionGroupById(201))
                .thenReturn(Optional.of(
                        new PositionGroupSummary(
                                201,
                                "Front of House",
                                List.of(new PositionSummary(101, "Server"))
                        )
                ));

        mockMvc.perform(get("/api/position-groups/201").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(201))
                .andExpect(jsonPath("$.name").value("Front of House"));

        verify(positionGroupService).getPositionGroupById(201);
    }

    @Test
    void getPositionGroupById_returnsNotFoundWhenMissing() throws Exception {
        when(positionGroupService.getPositionGroupById(201)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/position-groups/201").param("companyId", "1"))
                .andExpect(status().isNotFound());

        verify(positionGroupService).getPositionGroupById(201);
    }

    @Test
    void createPositionGroup_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/position-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 1,
                                  "description": "Front of House",
                                  "positionIds": [101, 102]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(positionGroupService).createPositionGroup("Front of House", List.of(101, 102));
    }

    @Test
    void updatePositionGroup_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/position-groups/201")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Updated Front of House",
                                  "positionIds": [102]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(positionGroupService).updatePositionGroup(eq(201), argThat(value ->
                value.description().equals("Updated Front of House")
                        && value.positionIds().equals(List.of(102))));
    }

    @Test
    void updatePositionGroup_returnsBadRequestWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(BAD_REQUEST, "One or more positions were not found for the company"))
                .when(positionGroupService)
                .updatePositionGroup(eq(201), argThat(value -> value.positionIds().equals(List.of(999))));

        mockMvc.perform(put("/api/position-groups/201")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Updated Front of House",
                                  "positionIds": [999]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletePositionGroup_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/position-groups/201").param("companyId", "1"))
                .andExpect(status().isNoContent());

        verify(positionGroupService).deletePositionGroup(201);
    }

    @Test
    void deletePositionGroup_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Position group not found"))
                .when(positionGroupService)
                .deletePositionGroup(201);

        mockMvc.perform(delete("/api/position-groups/201").param("companyId", "1"))
                .andExpect(status().isNotFound());
    }
}
