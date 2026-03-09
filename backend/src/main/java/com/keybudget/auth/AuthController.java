package com.keybudget.auth;

import com.keybudget.auth.dto.AuthResponse;
import com.keybudget.user.User;
import com.keybudget.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 3600;

    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final boolean secureCookie;

    public AuthController(
            JwtService jwtService,
            UserService userService,
            RefreshTokenService refreshTokenService,
            @Value("${app.cookie.secure}") boolean secureCookie) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null || !jwtService.isValidRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(refreshToken);
        User user = userService.findById(userId);

        String oldJti = jwtService.extractJti(refreshToken);
        try {
            refreshTokenService.validate(oldJti);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
        refreshTokenService.revoke(oldJti);

        String newAccessToken = jwtService.issueAccessToken(user);
        String newRefreshToken = jwtService.issueRefreshToken(user);

        refreshTokenService.store(jwtService.extractJti(newRefreshToken), userId,
                Instant.now().plusSeconds(REFRESH_TOKEN_MAX_AGE_SECONDS));

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/")
                .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(new AuthResponse(newAccessToken, 900));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            try {
                refreshTokenService.revoke(jwtService.extractJti(refreshToken));
            } catch (Exception ignored) {}
        }
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }
}
