package com.keybudget.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.category.dto.CategoryResponse;
import com.keybudget.category.dto.CreateCategoryRequest;
import com.keybudget.category.dto.UpdateCategoryRequest;
import com.keybudget.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    // -------------------------------------------------------------------------
    // GET /api/v1/categories
    // -------------------------------------------------------------------------

    @Test
    void getCategories_givenValidJwt_200() throws Exception {
        List<CategoryResponse> categories = List.of(
                new CategoryResponse(1L, "Housing", "home", "#4CAF50", CategoryType.EXPENSE, true),
                new CategoryResponse(2L, "My Custom", null, null, CategoryType.INCOME, false)
        );
        when(categoryService.getCategories(1L)).thenReturn(categories);

        mockMvc.perform(get("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Housing"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[1].isDefault").value(false));
    }

    @Test
    void getCategories_givenNoJwt_401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCategories_givenServiceThrows_500() throws Exception {
        when(categoryService.getCategories(1L)).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/categories
    // -------------------------------------------------------------------------

    @Test
    void createCategory_givenValidRequest_201() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("Rent", "home", "#FF0000", CategoryType.EXPENSE);
        CategoryResponse response = new CategoryResponse(10L, "Rent", "home", "#FF0000", CategoryType.EXPENSE, false);
        when(categoryService.createCategory(eq(1L), any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Rent"))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    void createCategory_givenBlankName_400() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("", null, null, CategoryType.EXPENSE);

        mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_givenNullType_400() throws Exception {
        String body = """
                {"name":"Valid Name","icon":null,"color":null,"type":null}
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_givenServiceThrows_500() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest("Rent", null, null, CategoryType.EXPENSE);
        when(categoryService.createCategory(eq(1L), any(CreateCategoryRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/categories/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateCategory_givenValidRequest_200() throws Exception {
        UpdateCategoryRequest req = new UpdateCategoryRequest("Updated", "star", "#000", CategoryType.EXPENSE);
        CategoryResponse response = new CategoryResponse(10L, "Updated", "star", "#000", CategoryType.EXPENSE, false);

        when(categoryService.updateCategory(eq(1L), eq(10L), any(UpdateCategoryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void updateCategory_givenNotFound_404() throws Exception {
        UpdateCategoryRequest req = new UpdateCategoryRequest("Name", null, null, CategoryType.EXPENSE);

        when(categoryService.updateCategory(eq(1L), eq(999L), any(UpdateCategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 999"));

        mockMvc.perform(put("/api/v1/categories/999")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateCategory_givenBlankName_400() throws Exception {
        UpdateCategoryRequest req = new UpdateCategoryRequest("", null, null, CategoryType.EXPENSE);

        mockMvc.perform(put("/api/v1/categories/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCategory_givenServiceThrows_500() throws Exception {
        UpdateCategoryRequest req = new UpdateCategoryRequest("Name", null, null, CategoryType.EXPENSE);

        when(categoryService.updateCategory(eq(1L), eq(10L), any(UpdateCategoryRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/api/v1/categories/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/categories/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteCategory_givenValidId_204() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L, 10L);

        mockMvc.perform(delete("/api/v1/categories/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCategory_givenNotFound_404() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found: 999"))
                .when(categoryService).deleteCategory(1L, 999L);

        mockMvc.perform(delete("/api/v1/categories/999")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteCategory_givenTransactionsExist_400() throws Exception {
        doThrow(new IllegalArgumentException("Cannot delete category with existing transactions"))
                .when(categoryService).deleteCategory(1L, 10L);

        mockMvc.perform(delete("/api/v1/categories/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void deleteCategory_givenServiceThrows_500() throws Exception {
        doThrow(new RuntimeException("DB error"))
                .when(categoryService).deleteCategory(1L, 10L);

        mockMvc.perform(delete("/api/v1/categories/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
