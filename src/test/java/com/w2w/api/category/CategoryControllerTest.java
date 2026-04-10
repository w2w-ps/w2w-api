package com.w2w.api.category;

import com.w2w.api.category.dto.CategoryResponse;
import com.w2w.api.category.dto.CategorySummary;
import com.w2w.api.config.GlobalExceptionHandler;
import com.w2w.api.config.TenantContext;
import com.w2w.api.config.exception.ForbiddenOperationException;
import com.w2w.api.config.exception.ResourceNotFoundException;
import com.w2w.api.login.JwtAuthFilter;
import com.w2w.api.login.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getCategories_returnsCategories() throws Exception {
        when(categoryService.getCategories("all"))
                .thenReturn(List.of(new CategorySummary(1, "Description", "ShortName")));

        mockMvc.perform(get("/api/categories").param("status", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].id").value(1))
                .andExpect(jsonPath("$.categories[0].description").value("Description"))
                .andExpect(jsonPath("$.categories[0].shortDesc").value("ShortName"));

        verify(categoryService).getCategories("all");
    }

    @Test
    void getCategoryById_returnsCategory() throws Exception {
        when(categoryService.getCategoryById(1))
                .thenReturn(new CategoryResponse(1, "Description", "ShortName", "09:00", "17:00", 12, (short) 1));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.shortName").value("ShortName"))
                .andExpect(jsonPath("$.startTime").value("09:00"))
                .andExpect(jsonPath("$.endTime").value("17:00"))
                .andExpect(jsonPath("$.positionId").value(12))
                .andExpect(jsonPath("$.color").value(1));

        verify(categoryService).getCategoryById(1);
    }

    @Test
    void getCategoryById_notFound_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found"))
                .when(categoryService)
                .getCategoryById(1);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isNotFound());

        verify(categoryService).getCategoryById(1);
    }

    @Test
    void createCategory_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "ShortName",
                                  "description": "Description",
                                  "startTime": "09:00",
                                  "endTime": "17:00",
                                  "positionId": 12,
                                  "color": 1
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryService).createCategory("ShortName", "Description", "09:00", "17:00", 12, (short) 1);
    }

    @Test
    void createCategory_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage categories."))
                .when(categoryService)
                .createCategory("ShortName", "Description", "09:00", "17:00", 12, (short) 1);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "ShortName",
                                  "description": "Description",
                                  "startTime": "09:00",
                                  "endTime": "17:00",
                                  "positionId": 12,
                                  "color": 1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCategory_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "New ShortName",
                                  "description": "New Description",
                                  "startTime": "10:00",
                                  "endTime": "18:00",
                                  "positionId": 13,
                                  "color": 2
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(categoryService).updateCategory(eq(1), any());
    }

    @Test
    void updateCategory_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage categories."))
                .when(categoryService)
                .updateCategory(eq(1), any());

        mockMvc.perform(put("/api/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shortName": "New ShortName",
                                  "description": "New Description",
                                  "startTime": "10:00",
                                  "endTime": "18:00",
                                  "positionId": 13,
                                  "color": 2
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategory_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(1);
    }

    @Test
    void deleteCategory_returnsForbiddenWhenServiceThrows() throws Exception {
        doThrow(new ForbiddenOperationException("You do not have permission to manage categories."))
                .when(categoryService)
                .deleteCategory(1);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isForbidden());
    }
}
