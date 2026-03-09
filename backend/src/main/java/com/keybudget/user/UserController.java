package com.keybudget.user;

import com.keybudget.user.dto.UpdateCurrencyRequest;
import com.keybudget.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me/currency")
    public ResponseEntity<UserProfileResponse> updateCurrency(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCurrencyRequest req) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(userService.updateCurrency(userId, req.currency()));
    }
}
