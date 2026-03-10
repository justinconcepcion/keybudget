package com.keybudget.user;

import com.keybudget.user.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void getMe_givenValidJwt_200() throws Exception {
        when(userService.getProfile(1L))
                .thenReturn(new UserProfileResponse(1L, "justin@example.com", "Justin", "https://pic.url", "USD"));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("justin@example.com"))
                .andExpect(jsonPath("$.name").value("Justin"));
    }

    @Test
    void getMe_givenNoJwt_401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_givenServiceThrowsUnexpectedly_500() throws Exception {
        when(userService.getProfile(1L))
                .thenThrow(new RuntimeException("DB connection lost"));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(j -> j.claim("userId", 1L))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
    }

    @Test
    void updateCurrency_givenValidCurrency_200() throws Exception {
        when(userService.updateCurrency(1L, "EUR"))
                .thenReturn(new UserProfileResponse(1L, "justin@example.com", "Justin", "https://pic.url", "EUR"));

        mockMvc.perform(put("/api/v1/users/me/currency")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredCurrency").value("EUR"));
    }

    @Test
    void updateCurrency_givenBlankCurrency_400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/currency")
                        .with(jwt().jwt(j -> j.claim("userId", 1L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCurrency_givenNoJwt_401() throws Exception {
        // CSRF token provided so the CSRF filter passes; auth check then returns 401
        mockMvc.perform(put("/api/v1/users/me/currency")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\"}"))
                .andExpect(status().isUnauthorized());
    }
}
