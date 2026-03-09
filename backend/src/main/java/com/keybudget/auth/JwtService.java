package com.keybudget.auth;

import com.keybudget.user.User;

public interface JwtService {
    String issueAccessToken(User user);
    String issueRefreshToken(User user);
    Long extractUserId(String token);
    boolean isValidRefreshToken(String token);
}
