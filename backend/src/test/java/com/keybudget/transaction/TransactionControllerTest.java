package com.keybudget.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.transaction.dto.CategoryTotal;
import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    // -------------------------------------------------------------------------
    // GET /api/v1/transactions
    // -------------------------------------------------------------------------

    @Test
    void getTransactions_givenValidJwt_200() throws Exception {
        TransactionResponse tx = new TransactionResponse(
                1L, new BigDecimal("100.00"), "Groceries",
                LocalDate.of(2026, 3, 10), TransactionType.EXPENSE, 5L, "Food");

        when(transactionService.getTransactions(
                eq(1L), any(LocalDate.class), any(LocalDate.class),
                isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));

        mockMvc.perform(get("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].categoryName").value("Food"));
    }

    @Test
    void getTransactions_givenNoJwt_401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTransactions_givenServiceThrows_500() throws Exception {
        when(transactionService.getTransactions(
                eq(1L), any(LocalDate.class), any(LocalDate.class),
                isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/transactions
    // -------------------------------------------------------------------------

    @Test
    void createTransaction_givenValidRequest_201() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("50.00"), "Dinner", LocalDate.of(2026, 3, 15),
                TransactionType.EXPENSE, 5L);
        TransactionResponse response = new TransactionResponse(
                20L, new BigDecimal("50.00"), "Dinner",
                LocalDate.of(2026, 3, 15), TransactionType.EXPENSE, 5L, "Food");

        when(transactionService.createTransaction(eq(1L), any(CreateTransactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.amount").value(50.00));
    }

    @Test
    void createTransaction_givenNullAmount_400() throws Exception {
        String body = """
                {"amount":null,"description":"Test","date":"2026-03-15","type":"EXPENSE","categoryId":5}
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_givenServiceThrows_500() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("50.00"), null, LocalDate.of(2026, 3, 15),
                TransactionType.EXPENSE, 5L);
        when(transactionService.createTransaction(eq(1L), any(CreateTransactionRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/transactions/summary
    // -------------------------------------------------------------------------

    @Test
    void getMonthlySummary_givenValidJwt_200() throws Exception {
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                new BigDecimal("3000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2500.00"),
                List.of(new CategoryTotal(5L, "Food", new BigDecimal("500.00")))
        );
        when(transactionService.getMonthlySummary(eq(1L), any(YearMonth.class)))
                .thenReturn(summary);

        mockMvc.perform(get("/api/v1/transactions/summary?month=2026-03")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(3000.00))
                .andExpect(jsonPath("$.totalExpenses").value(500.00))
                .andExpect(jsonPath("$.netSavings").value(2500.00))
                .andExpect(jsonPath("$.byCategory.length()").value(1));
    }

    @Test
    void getMonthlySummary_givenNoJwt_401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMonthlySummary_givenServiceThrows_500() throws Exception {
        when(transactionService.getMonthlySummary(eq(1L), any(YearMonth.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/v1/transactions/summary")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }
}
