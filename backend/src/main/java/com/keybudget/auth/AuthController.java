package com.keybudget.auth;

import com.keybudget.auth.dto.AuthResponse;
import com.keybudget.user.User;
import com.keybudget.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
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
        String oldJti = jwtService.extractJti(refreshToken);

        // Validate and atomically revoke BEFORE user lookup to ensure
        // reuse detection triggers even for deleted users
        RefreshToken storedToken;
        try {
            storedToken = refreshTokenService.validateAndRevoke(oldJti);
        } catch (InvalidRefreshTokenException e) {
            log.warn("Refresh token validation failed for userId={}: {}", userId, e.getMessage());
            return ResponseEntity.status(401).build();
        }
        if (!storedToken.getUserId().equals(userId)) {
            return ResponseEntity.status(401).build();
        }

        User user;
        try {
            user = userService.findById(userId);
        } catch (Exception e) {
            log.warn("User not found during refresh for userId={}", userId);
            return ResponseEntity.status(401).build();
        }

        String newAccessToken = jwtService.issueAccessToken(user);
        String newRefreshToken = jwtService.issueRefreshToken(user);

        refreshTokenService.store(jwtService.extractJti(newRefreshToken), userId,
                Instant.now().plusSeconds(REFRESH_TOKEN_MAX_AGE_SECONDS), storedToken.getFamilyId());

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
        if (refreshToken != null && jwtService.isValidRefreshToken(refreshToken)) {
            try {
                refreshTokenService.validateAndRevoke(jwtService.extractJti(refreshToken));
            } catch (InvalidRefreshTokenException e) {
                log.debug("Logout with invalid refresh token: {}", e.getMessage());
            }
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
