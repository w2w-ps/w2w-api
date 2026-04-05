package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryResponse;
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

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getCategories_returnsCategories() throws Exception {
        when(categoryService.getCategories(1, "all"))
                .thenReturn(List.of(
                        new CategorySummary(4, "Floor", "FLR"),
                        new CategorySummary(5, "Front", "FRT")
                ));

        mockMvc.perform(get("/api/categories").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].id").value(4))
                .andExpect(jsonPath("$.categories[0].name").value("Floor"))
                .andExpect(jsonPath("$.categories[0].shortName").value("FLR"))
                .andExpect(jsonPath("$.categories[1].id").value(5))
                .andExpect(jsonPath("$.categories[1].name").value("Front"))
                .andExpect(jsonPath("$.categories[1].shortName").value("FRT"));

        verify(categoryService).getCategories(1, "all");
    }

    @Test
    void getCategories_withStatusFilter_returnsCategories() throws Exception {
        when(categoryService.getCategories(1, "inactive"))
                .thenReturn(List.of(new CategorySummary(6, "Archived Floor", "AFR")));

        mockMvc.perform(get("/api/categories").param("companyId", "1").param("status", "inactive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].id").value(6))
                .andExpect(jsonPath("$.categories[0].name").value("Archived Floor"))
                .andExpect(jsonPath("$.categories[0].shortName").value("AFR"));

        verify(categoryService).getCategories(1, "inactive");
    }

    @Test
    void getCategories_returnsBadRequestWhenStatusInvalid() throws Exception {
        doThrow(new ResponseStatusException(BAD_REQUEST, "Unsupported status filter"))
                .when(categoryService)
                .getCategories(1, "archived");

        mockMvc.perform(get("/api/categories").param("companyId", "1").param("status", "archived"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCategoryById_returnsCategoryWhenFound() throws Exception {
        when(categoryService.getCategoryById(4, 1))
                .thenReturn(Optional.of(new CategoryResponse(4, "Floor", "FLR", "08:00", "17:00", 12, (short) 3)));

        mockMvc.perform(get("/api/categories/4").param("companyId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(4))
                .andExpect(jsonPath("$.description").value("Floor"))
                .andExpect(jsonPath("$.shortName").value("FLR"))
                .andExpect(jsonPath("$.startTime").value("08:00"))
                .andExpect(jsonPath("$.endTime").value("17:00"))
                .andExpect(jsonPath("$.skillId").value(12))
                .andExpect(jsonPath("$.color").value(3));

        verify(categoryService).getCategoryById(4, 1);
    }

    @Test
    void getCategoryById_returnsNotFoundWhenMissing() throws Exception {
        when(categoryService.getCategoryById(4, 1)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/categories/4").param("companyId", "1"))
                .andExpect(status().isNotFound());

        verify(categoryService).getCategoryById(4, 1);
    }

    @Test
    void createCategory_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyId": 1,
                                  "shortName": "FLR",
                                  "description": "Floor",
                                  "startTime": "08:00",
                                  "endTime": "17:00",
                                  "skillId": 12,
                                  "color": 3
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryService).createCategory(argThat(value ->
                value.companyId().equals(1)
                        && value.shortName().equals("FLR")
                        && value.description().equals("Floor")
                        && value.startTime().equals("08:00")
                        && value.endTime().equals("17:00")
                        && value.skillId().equals(12)
                        && value.color().equals((short) 3)));
    }

    @Test
    void updateCategory_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/categories/4")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "FLOOR",
                                  "description": "Updated Floor",
                                  "startTime": "09:00",
                                  "endTime": "18:00",
                                  "skillId": 12,
                                  "color": 4
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryService).updateCategory(eq(4), eq(1), argThat(value ->
                value.shortName().equals("FLOOR")
                        && value.description().equals("Updated Floor")
                        && value.startTime().equals("09:00")
                        && value.endTime().equals("18:00")
                        && value.skillId().equals(12)
                        && value.color().equals((short) 4)));
    }

    @Test
    void updateCategory_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Category not found"))
                .when(categoryService)
                .updateCategory(eq(4), eq(1), argThat(value -> value.description().equals("Updated Floor")));

        mockMvc.perform(put("/api/categories/4")
                        .param("companyId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "FLOOR",
                                  "description": "Updated Floor",
                                  "startTime": "09:00",
                                  "endTime": "18:00",
                                  "skillId": 12,
                                  "color": 4
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/categories/4").param("companyId", "1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(4, 1);
    }

    @Test
    void deleteCategory_returnsNotFoundWhenServiceThrows() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Category not found"))
                .when(categoryService)
                .deleteCategory(4, 1);

        mockMvc.perform(delete("/api/categories/4").param("companyId", "1"))
                .andExpect(status().isNotFound());
    }
}
