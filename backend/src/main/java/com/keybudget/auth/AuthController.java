package com.keybudget.auth;

import com.keybudget.auth.dto.AuthResponse;
import com.keybudget.auth.dto.RefreshRequest;
import com.keybudget.user.User;
import com.keybudget.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        if (!jwtService.isValidRefreshToken(request.refreshToken())) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.extractUserId(request.refreshToken());
        User user = userService.findById(userId);

        String newAccessToken = jwtService.issueAccessToken(user);
        String newRefreshToken = jwtService.issueRefreshToken(user);
        return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken, 900));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Stateless — client discards tokens. Future: add refresh token revocation list.
        return ResponseEntity.noContent().build();
    }
}
