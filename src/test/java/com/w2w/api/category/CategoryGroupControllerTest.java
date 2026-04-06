package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryGroupSummary;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
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

@WebMvcTest(CategoryGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryGroupService categoryGroupService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getCategoryGroups_returnsGroups() throws Exception {
        when(categoryGroupService.getCategoryGroups(1, "all"))
                .thenReturn(List.of(
                        new CategoryGroupSummary(
                                301,
                                "Standard Shifts",
                                List.of(new CategorySummary(4, "Floor", "FLR"))
                        )
                ));

        mockMvc.perform(get("/api/category-groups").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryGroups[0].id").value(301))
                .andExpect(jsonPath("$.categoryGroups[0].name").value("Standard Shifts"))
                .andExpect(jsonPath("$.categoryGroups[0].categories[0].id").value(4))
                .andExpect(jsonPath("$.categoryGroups[0].categories[0].name").value("Floor"))
                .andExpect(jsonPath("$.categoryGroups[0].categories[0].shortName").value("FLR"));

        verify(categoryGroupService).getCategoryGroups(1, "all");
    }

    @Test
    void getCategoryGroups_withStatusFilter_returnsGroups() throws Exception {
        when(categoryGroupService.getCategoryGroups(1, "non-active"))
                .thenReturn(List.of(
                        new CategoryGroupSummary(
                                302,
                                "Archived Shifts",
                                List.of(new CategorySummary(5, "Front", "FRT"))
                        )
                ));

        mockMvc.perform(get("/api/category-groups").param("companyId", "1").param("status", "non-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryGroups[0].id").value(302))
                .andExpect(jsonPath("$.categoryGroups[0].name").value("Archived Shifts"));

        verify(categoryGroupService).getCategoryGroups(1, "non-active");
    }

    @Test
    void getCategoryGroups_returnsBadRequestWhenStatusInvalid() throws Exception {
        doThrow(new ResponseStatusException(BAD_REQUEST, "Unsupported status filter"))
                .when(categoryGroupService)
                .getCategoryGroups(1, "archived");

        mockMvc.perform(get("/api/category-groups").param("companyId", "1").param("status", "archived"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategoryGroupById_returnsGroupWhenFound() throws Exception {
        when(categoryGroupService.getCategoryGroupById(301, 1))
                .thenReturn(Optional.of(
                        new CategoryGroupSummary(
                                301,
                                "Standard Shifts",
                                List.of(new CategorySummary(4, "Floor", "FLR"))
                        )
                ));

        mockMvc.perform(get("/api/category-groups/301").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(301))
                .andExpect(jsonPath("$.name").value("Standard Shifts"));

        verify(categoryGroupService).getCategoryGroupById(301, 1);
    }

    @Test
    void getCategoryGroupById_returnsNotFoundWhenMissing() throws Exception {
        when(categoryGroupService.getCategoryGroupById(301, 1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/category-groups/301").param("companyId", "1"))
                .andExpect(status().isNotFound());

        verify(categoryGroupService).getCategoryGroupById(301, 1);
    }

    @Test
    void createCategoryGroup_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/category-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 1,
                                  "description": "Standard Shifts",
                                  "categoryIds": [4, 5]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryGroupService).createCategoryGroup(argThat(value ->
                value.companyId().equals(1)
                        && value.description().equals("Standard Shifts")
                        && value.categoryIds().equals(List.of(4, 5))));
    }

    @Test
    void updateCategoryGroup_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/category-groups/301")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Updated Standard Shifts",
                                  "categoryIds": [5]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryGroupService).updateCategoryGroup(eq(301), eq(1), argThat(value ->
                value.description().equals("Updated Standard Shifts")
                        && value.categoryIds().equals(List.of(5))));
    }

    @Test
    void updateCategoryGroup_returnsBadRequestWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(BAD_REQUEST, "One or more categories were not found for the company"))
                .when(categoryGroupService)
                .updateCategoryGroup(eq(301), eq(1), argThat(value -> value.categoryIds().equals(List.of(999))));

        mockMvc.perform(put("/api/category-groups/301")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "Updated Standard Shifts",
                                  "categoryIds": [999]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategoryGroup_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/category-groups/301").param("companyId", "1"))
                .andExpect(status().isNoContent());

        verify(categoryGroupService).deleteCategoryGroup(301, 1);
    }

    @Test
    void deleteCategoryGroup_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Category group not found"))
                .when(categoryGroupService)
                .deleteCategoryGroup(301, 1);

        mockMvc.perform(delete("/api/category-groups/301").param("companyId", "1"))
                .andExpect(status().isNotFound());
    }
}
