package com.keybudget.auth;

import com.keybudget.user.User;
import com.keybudget.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 3600;

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String frontendUrl;
    private final boolean secureCookie;

    public OAuth2SuccessHandler(
            UserService userService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.cookie.secure}") boolean secureCookie) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.frontendUrl = frontendUrl;
        this.secureCookie = secureCookie;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        User user = userService.upsertFromGoogle(
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getFullName(),
                oidcUser.getPicture()
        );

        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = jwtService.issueRefreshToken(user);

        String jti = jwtService.extractJti(refreshToken);
        refreshTokenService.store(jti, user.getId(), Instant.now().plusSeconds(7 * 24 * 3600));

        // Refresh token: HttpOnly cookie — not accessible to JavaScript
        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/")
                .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Access token: URL fragment — fragments are never sent to servers or logged
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl + "/auth/callback#token=" + encodedToken);
    }
}
