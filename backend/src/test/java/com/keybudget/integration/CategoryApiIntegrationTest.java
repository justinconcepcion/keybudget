package com.keybudget.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.category.CategoryType;
import com.keybudget.category.dto.CreateCategoryRequest;
import com.keybudget.category.dto.UpdateCategoryRequest;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration tests for the Category API.
 * Uses real H2 database with Hibernate-managed schema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setGoogleSub("google-test-123");
        user.setEmail("test@example.com");
        user.setName("Test User");
        userId = userRepository.save(user).getId();
    }

    @Test
    void createAndGetCategories_givenValidData_201then200() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest(
                "Groceries", "cart", "#4CAF50", CategoryType.EXPENSE);

        // Create
        MvcResult createResult = mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andReturn();

        // List — should include the created category plus system defaults
        mockMvc.perform(get("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'Groceries')]").exists());
    }

    @Test
    void updateCategory_givenExistingCategory_200() throws Exception {
        // Create first
        CreateCategoryRequest createReq = new CreateCategoryRequest(
                "Food", null, null, CategoryType.EXPENSE);
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long catId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Update
        UpdateCategoryRequest updateReq = new UpdateCategoryRequest(
                "Dining Out", "fork", "#FF5722", CategoryType.EXPENSE);

        mockMvc.perform(put("/api/v1/categories/" + catId)
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dining Out"))
                .andExpect(jsonPath("$.icon").value("fork"));
    }

    @Test
    void deleteCategory_givenExistingCategory_204() throws Exception {
        // Create
        CreateCategoryRequest req = new CreateCategoryRequest(
                "Temp", null, null, CategoryType.EXPENSE);
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long catId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Delete
        mockMvc.perform(delete("/api/v1/categories/" + catId)
                        .with(jwt().jwt(j -> j.claim("userId", userId))))
                .andExpect(status().isNoContent());

        // Verify gone — listing should not contain "Temp"
        mockMvc.perform(get("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Temp')]").doesNotExist());
    }

    @Test
    void updateCategory_givenWrongUser_404() throws Exception {
        // Create as user A
        CreateCategoryRequest req = new CreateCategoryRequest(
                "Private", null, null, CategoryType.INCOME);
        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Long catId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Create user B
        User userB = new User();
        userB.setGoogleSub("google-other-456");
        userB.setEmail("other@example.com");
        userB.setName("Other User");
        Long userBId = userRepository.save(userB).getId();

        // Try to update as user B — should 404 (not visible)
        UpdateCategoryRequest updateReq = new UpdateCategoryRequest(
                "Hacked", null, null, CategoryType.INCOME);

        mockMvc.perform(put("/api/v1/categories/" + catId)
                        .with(jwt().jwt(j -> j.claim("userId", userBId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_givenNoAuth_401() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest(
                "Test", null, null, CategoryType.EXPENSE);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_givenInvalidData_400() throws Exception {
        CreateCategoryRequest req = new CreateCategoryRequest(
                "", null, null, CategoryType.EXPENSE);

        mockMvc.perform(post("/api/v1/categories")
                        .with(jwt().jwt(j -> j.claim("userId", userId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
