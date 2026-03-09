package com.keybudget.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
import com.keybudget.transaction.TransactionType;
import com.keybudget.transaction.dto.CreateTransactionRequest;
import com.keybudget.transaction.dto.UpdateTransactionRequest;
import com.keybudget.user.User;
import com.keybudget.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests for the Transaction API.
 * Uses real H2 database with Hibernate-managed schema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long userId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setGoogleSub("google-tx-test");
        user.setEmail("tx@example.com");
        user.setName("TX Test User");
        userId = userRepository.save(user).getId();

        Category category = new Category();
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);
        category.setUserId(userId);
        categoryId = categoryRepository.save(category).getId();
    }

    @Test
    void createAndListTransactions_givenValidData_201then200() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("42.50"), "Lunch", LocalDate.of(2026, 3, 9),
                TransactionType.EXPENSE, categoryId);

        // Create
        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(42.50))
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.categoryName").value("Food"));

        // List
        mockMvc.perform(get("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .param("start", "2026-03-01")
                        .param("end", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Lunch"));
    }

    @Test
    void updateTransaction_givenExisting_200() throws Exception {
        // Create
        CreateTransactionRequest createReq = new CreateTransactionRequest(
                new BigDecimal("10.00"), "Coffee", LocalDate.of(2026, 3, 9),
                TransactionType.EXPENSE, categoryId);
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Update
        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(
                new BigDecimal("15.00"), "Fancy Coffee", LocalDate.of(2026, 3, 9),
                TransactionType.EXPENSE, categoryId);

        mockMvc.perform(put("/api/v1/transactions/" + txId)
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(15.00))
                .andExpect(jsonPath("$.description").value("Fancy Coffee"));
    }

    @Test
    void deleteTransaction_givenExisting_204() throws Exception {
        // Create
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("5.00"), "Delete me", LocalDate.of(2026, 3, 9),
                TransactionType.EXPENSE, categoryId);
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Delete
        mockMvc.perform(delete("/api/v1/transactions/" + txId)
                        .with(jwt().jwt(j -> j.claim("userId", userId))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMonthlySummary_givenTransactions_200() throws Exception {
        // Create income
        Category incomeCat = new Category();
        incomeCat.setName("Salary");
        incomeCat.setType(CategoryType.INCOME);
        incomeCat.setUserId(userId);
        Long incomeCatId = categoryRepository.save(incomeCat).getId();

        CreateTransactionRequest income = new CreateTransactionRequest(
                new BigDecimal("5000.00"), "March salary", LocalDate.of(2026, 3, 1),
                TransactionType.INCOME, incomeCatId);
        CreateTransactionRequest expense = new CreateTransactionRequest(
                new BigDecimal("200.00"), "Groceries", LocalDate.of(2026, 3, 5),
                TransactionType.EXPENSE, categoryId);

        mockMvc.perform(post("/api/v1/transactions")
                .with(jwt().jwt(j -> j.claim("userId", userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(income)));
        mockMvc.perform(post("/api/v1/transactions")
                .with(jwt().jwt(j -> j.claim("userId", userId)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(expense)));

        // Get summary
        mockMvc.perform(get("/api/v1/transactions/summary")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .param("month", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses").value(200.00))
                .andExpect(jsonPath("$.netSavings").value(4800.00));
    }

    @Test
    void createTransaction_givenNoAuth_401() throws Exception {
        CreateTransactionRequest req = new CreateTransactionRequest(
                new BigDecimal("10.00"), "Test", LocalDate.of(2026, 3, 9),
                TransactionType.EXPENSE, categoryId);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTransaction_givenInvalidData_400() throws Exception {
        // Negative amount
        String body = """
                {"amount": -10, "date": "2026-03-09", "type": "EXPENSE", "categoryId": 1}
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
