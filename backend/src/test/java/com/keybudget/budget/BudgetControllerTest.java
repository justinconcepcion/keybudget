package com.keybudget.budget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.keybudget.budget.dto.BudgetResponse;
import com.keybudget.budget.dto.CreateBudgetRequest;
import com.keybudget.budget.dto.UpdateBudgetRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BudgetService budgetService;

    private BudgetResponse sampleBudgetResponse() {
        return new BudgetResponse(
                10L, 5L, "Food", "#FF9800",
                YearMonth.of(2026, 3),
                new BigDecimal("500.00"),
                new BigDecimal("200.00"),
                new BigDecimal("300.00")
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/budgets
    // -------------------------------------------------------------------------

    @Test
    void getBudgets_givenValidJwt_200() throws Exception {
        when(budgetService.getBudgets(eq(1L), any(YearMonth.class)))
                .thenReturn(List.of(sampleBudgetResponse()));

        mockMvc.perform(get("/api/v1/budgets?month=2026-03")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].categoryName").value("Food"))
                .andExpect(jsonPath("$[0].spentAmount").value(200.00))
                .andExpect(jsonPath("$[0].remainingAmount").value(300.00));
    }

    @Test
    void getBudgets_givenNoJwt_401() throws Exception {
        mockMvc.perform(get("/api/v1/budgets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBudgets_givenServiceThrows_500() throws Exception {
        when(budgetService.getBudgets(eq(1L), any(YearMonth.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/budgets
    // -------------------------------------------------------------------------

    @Test
    void createBudget_givenValidRequest_201() throws Exception {
        // Use raw JSON to avoid YearMonth serialization issues in test
        String body = """
                {"categoryId":5,"monthYear":"2026-03","limitAmount":500.00}
                """;
        when(budgetService.createBudget(eq(1L), any(CreateBudgetRequest.class)))
                .thenReturn(sampleBudgetResponse());

        mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createBudget_givenNullCategoryId_400() throws Exception {
        String body = """
                {"categoryId":null,"monthYear":"2026-03","limitAmount":500.00}
                """;

        mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBudget_givenServiceThrows_500() throws Exception {
        String body = """
                {"categoryId":5,"monthYear":"2026-03","limitAmount":500.00}
                """;
        when(budgetService.createBudget(eq(1L), any(CreateBudgetRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/budgets/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateBudget_givenValidRequest_200() throws Exception {
        UpdateBudgetRequest req = new UpdateBudgetRequest(new BigDecimal("600.00"));
        when(budgetService.updateBudget(eq(1L), eq(10L), any(UpdateBudgetRequest.class)))
                .thenReturn(sampleBudgetResponse());

        mockMvc.perform(put("/api/v1/budgets/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateBudget_givenNullLimitAmount_400() throws Exception {
        String body = """
                {"limitAmount":null}
                """;

        mockMvc.perform(put("/api/v1/budgets/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBudget_givenServiceThrows_500() throws Exception {
        UpdateBudgetRequest req = new UpdateBudgetRequest(new BigDecimal("600.00"));
        when(budgetService.updateBudget(eq(1L), eq(10L), any(UpdateBudgetRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/api/v1/budgets/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/budgets/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteBudget_givenValidRequest_204() throws Exception {
        doNothing().when(budgetService).deleteBudget(1L, 10L);

        mockMvc.perform(delete("/api/v1/budgets/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteBudget_givenNoJwt_401() throws Exception {
        // CSRF token provided so the CSRF filter passes; auth check then returns 401
        mockMvc.perform(delete("/api/v1/budgets/10").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteBudget_givenServiceThrows_500() throws Exception {
        doThrow(new RuntimeException("DB error")).when(budgetService).deleteBudget(1L, 10L);

        mockMvc.perform(delete("/api/v1/budgets/10")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
