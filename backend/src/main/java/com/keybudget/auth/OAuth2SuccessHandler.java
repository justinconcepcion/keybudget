package com.keybudget.auth;

import com.keybudget.user.User;
import com.keybudget.user.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 3600;

    private final UserService userService;
    private final JwtService jwtService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(
            UserService userService,
            JwtService jwtService,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
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

        // Refresh token: HttpOnly cookie — not accessible to JavaScript
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(request.isSecure());
        refreshCookie.setPath("/api/v1/auth/refresh");
        refreshCookie.setMaxAge(REFRESH_TOKEN_MAX_AGE_SECONDS);
        response.addCookie(refreshCookie);

        // Access token: URL fragment — fragments are never sent to servers or logged
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl + "/auth/callback#token=" + encodedToken);
    }
}
