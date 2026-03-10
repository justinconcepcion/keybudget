package com.keybudget.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.transaction.dto.CategoryTotal;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.CsvImportResult;
import com.keybudget.transaction.dto.MonthlySummaryResponse;
import com.keybudget.transaction.dto.TransactionResponse;
import com.keybudget.transaction.dto.UpdateTransactionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private CsvImportService csvImportService;

    // -------------------------------------------------------------------------
    // POST /api/v1/transactions/import
    // -------------------------------------------------------------------------

    @Test
    void importCsv_givenValidFile_200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n".getBytes(StandardCharsets.UTF_8));

        when(csvImportService.importCsv(eq(1L), any(), isNull()))
                .thenReturn(new CsvImportResult(1, 1, 0, List.of()));

        mockMvc.perform(multipart("/api/v1/transactions/import")
                        .file(file)
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.totalRows").value(1));
    }

    @Test
    void importCsv_givenNoJwt_401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                "Date,Description,Amount\n".getBytes(StandardCharsets.UTF_8));

        // CSRF token provided so the CSRF filter passes; auth check then returns 401
        mockMvc.perform(multipart("/api/v1/transactions/import").file(file).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importCsv_givenServiceThrows_500() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                "Date,Description,Amount\n2026-03-01,Coffee,-5.50\n".getBytes(StandardCharsets.UTF_8));

        when(csvImportService.importCsv(eq(1L), any(), isNull()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(multipart("/api/v1/transactions/import")
                        .file(file)
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError());
    }

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
    // PUT /api/v1/transactions/{id}
    // -------------------------------------------------------------------------

    @Test
    void updateTransaction_givenValidRequest_200() throws Exception {
        UpdateTransactionRequest req = new UpdateTransactionRequest(
                new BigDecimal("75.00"), "Updated dinner", LocalDate.of(2026, 3, 15),
                TransactionType.EXPENSE, 5L);
        TransactionResponse response = new TransactionResponse(
                20L, new BigDecimal("75.00"), "Updated dinner",
                LocalDate.of(2026, 3, 15), TransactionType.EXPENSE, 5L, "Food");

        when(transactionService.updateTransaction(eq(1L), eq(20L), any(UpdateTransactionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/transactions/20")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.description").value("Updated dinner"));
    }

    @Test
    void updateTransaction_givenNotFound_404() throws Exception {
        UpdateTransactionRequest req = new UpdateTransactionRequest(
                new BigDecimal("75.00"), "Test", LocalDate.of(2026, 3, 15),
                TransactionType.EXPENSE, 5L);

        when(transactionService.updateTransaction(eq(1L), eq(999L), any(UpdateTransactionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Transaction not found: 999"));

        mockMvc.perform(put("/api/v1/transactions/999")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void updateTransaction_givenNullAmount_400() throws Exception {
        String body = """
                {"amount":null,"description":"Test","date":"2026-03-15","type":"EXPENSE","categoryId":5}
                """;

        mockMvc.perform(put("/api/v1/transactions/20")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTransaction_givenServiceThrows_500() throws Exception {
        UpdateTransactionRequest req = new UpdateTransactionRequest(
                new BigDecimal("75.00"), null, LocalDate.of(2026, 3, 15),
                TransactionType.EXPENSE, 5L);

        when(transactionService.updateTransaction(eq(1L), eq(20L), any(UpdateTransactionRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/api/v1/transactions/20")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/transactions/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteTransaction_givenValidId_204() throws Exception {
        doNothing().when(transactionService).deleteTransaction(1L, 20L);

        mockMvc.perform(delete("/api/v1/transactions/20")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTransaction_givenNotFound_404() throws Exception {
        doThrow(new ResourceNotFoundException("Transaction not found: 999"))
                .when(transactionService).deleteTransaction(1L, 999L);

        mockMvc.perform(delete("/api/v1/transactions/999")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void deleteTransaction_givenNoJwt_403() throws Exception {
        mockMvc.perform(delete("/api/v1/transactions/20"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteTransaction_givenServiceThrows_500() throws Exception {
        doThrow(new RuntimeException("DB error"))
                .when(transactionService).deleteTransaction(1L, 20L);

        mockMvc.perform(delete("/api/v1/transactions/20")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
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
