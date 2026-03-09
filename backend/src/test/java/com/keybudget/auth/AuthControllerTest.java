package com.keybudget.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.auth.dto.RefreshRequest;
import com.keybudget.user.User;
import com.keybudget.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @Test
    void refresh_givenValidRefreshToken_200() throws Exception {
        User user = new User();
        when(jwtService.isValidRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh-token")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(user);
        when(jwtService.issueAccessToken(any())).thenReturn("new-access-token");
        when(jwtService.issueRefreshToken(any())).thenReturn("new-refresh-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("valid-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void refresh_givenInvalidRefreshToken_401() throws Exception {
        when(jwtService.isValidRefreshToken("bad-token")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("bad-token"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_givenMissingBody_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_givenValidJwt_204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
