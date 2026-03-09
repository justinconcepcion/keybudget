package com.keybudget.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.budget.dto.CreateBudgetRequest;
import com.keybudget.budget.dto.UpdateBudgetRequest;
import com.keybudget.category.Category;
import com.keybudget.category.CategoryRepository;
import com.keybudget.category.CategoryType;
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
import java.time.YearMonth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests for the Budget API.
 * Uses real H2 database with Hibernate-managed schema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BudgetApiIntegrationTest {

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
        user.setGoogleSub("google-budget-test");
        user.setEmail("budget@example.com");
        user.setName("Budget Test User");
        userId = userRepository.save(user).getId();

        Category category = new Category();
        category.setName("Food");
        category.setType(CategoryType.EXPENSE);
        category.setUserId(userId);
        categoryId = categoryRepository.save(category).getId();
    }

    @Test
    void createAndListBudgets_givenValidData_201then200() throws Exception {
        CreateBudgetRequest req = new CreateBudgetRequest(
                categoryId, YearMonth.of(2026, 3), new BigDecimal("500.00"));

        // Create
        mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitAmount").value(500.00))
                .andExpect(jsonPath("$.categoryName").value("Food"));

        // List
        mockMvc.perform(get("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .param("month", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].categoryName").value("Food"));
    }

    @Test
    void updateBudget_givenExisting_200() throws Exception {
        // Create
        CreateBudgetRequest createReq = new CreateBudgetRequest(
                categoryId, YearMonth.of(2026, 3), new BigDecimal("300.00"));
        MvcResult result = mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long budgetId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Update limit
        UpdateBudgetRequest updateReq = new UpdateBudgetRequest(new BigDecimal("750.00"));

        mockMvc.perform(put("/api/v1/budgets/" + budgetId)
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitAmount").value(750.00));
    }

    @Test
    void deleteBudget_givenExisting_204() throws Exception {
        CreateBudgetRequest req = new CreateBudgetRequest(
                categoryId, YearMonth.of(2026, 3), new BigDecimal("100.00"));
        MvcResult result = mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long budgetId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(delete("/api/v1/budgets/" + budgetId)
                        .with(jwt().jwt(j -> j.claim("userId", userId))))
                .andExpect(status().isNoContent());
    }

    @Test
    void createBudget_givenDuplicate_409() throws Exception {
        CreateBudgetRequest req = new CreateBudgetRequest(
                categoryId, YearMonth.of(2026, 3), new BigDecimal("500.00"));

        // First create
        mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Duplicate — same user, category, month
        mockMvc.perform(post("/api/v1/budgets")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createBudget_givenNoAuth_401() throws Exception {
        CreateBudgetRequest req = new CreateBudgetRequest(
                categoryId, YearMonth.of(2026, 3), new BigDecimal("500.00"));

        mockMvc.perform(post("/api/v1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
