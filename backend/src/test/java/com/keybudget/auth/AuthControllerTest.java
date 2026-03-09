package com.keybudget.auth;

import com.keybudget.config.SecurityConfig;
import com.keybudget.user.User;
import com.keybudget.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cookie.secure=false")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/refresh
    // -------------------------------------------------------------------------

    @Test
    void refresh_givenValidCookie_200() throws Exception {
        User user = new User();
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUserId(1L);
        storedToken.setFamilyId("family-1");
        when(jwtService.isValidRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh-token")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(user);
        when(jwtService.extractJti("valid-refresh-token")).thenReturn("old-jti");
        when(refreshTokenService.validateAndRevoke("old-jti")).thenReturn(storedToken);
        when(jwtService.issueAccessToken(any())).thenReturn("new-access-token");
        when(jwtService.issueRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtService.extractJti("new-refresh-token")).thenReturn("new-jti");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=new-refresh-token")))
                .andExpect(header().string("Set-Cookie", containsString("Path=/api/v1/auth/")));

        verify(refreshTokenService).store(eq("new-jti"), eq(1L), any(), eq("family-1"));
    }

    @Test
    void refresh_givenInvalidCookie_401() throws Exception {
        when(jwtService.isValidRefreshToken("bad-token")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "bad-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_givenNoCookie_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_givenRevokedToken_401() throws Exception {
        when(jwtService.isValidRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-refresh-token")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(new User());
        when(jwtService.extractJti("valid-refresh-token")).thenReturn("jti");
        when(refreshTokenService.validateAndRevoke("jti"))
                .thenThrow(new InvalidRefreshTokenException("Refresh token has been revoked"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_givenReusedToken_401() throws Exception {
        when(jwtService.isValidRefreshToken("reused-token")).thenReturn(true);
        when(jwtService.extractUserId("reused-token")).thenReturn(1L);
        when(userService.findById(1L)).thenReturn(new User());
        when(jwtService.extractJti("reused-token")).thenReturn("jti");
        when(refreshTokenService.validateAndRevoke("jti"))
                .thenThrow(new InvalidRefreshTokenException("Refresh token already consumed"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "reused-token")))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/auth/logout
    // -------------------------------------------------------------------------

    @Test
    void logout_givenCookiePresent_204AndCookieCleared() throws Exception {
        when(jwtService.extractJti("some-token")).thenReturn("jti");
        RefreshToken storedToken = new RefreshToken();
        when(refreshTokenService.validateAndRevoke("jti")).thenReturn(storedToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "some-token")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    void logout_givenNoCookie_204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
