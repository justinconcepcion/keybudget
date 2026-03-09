package com.keybudget.auth;

import java.time.Instant;

public interface RefreshTokenService {
    RefreshToken store(String jti, Long userId, Instant expiresAt);
    RefreshToken validate(String jti);
    void revoke(String jti);
}
