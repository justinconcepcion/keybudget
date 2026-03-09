package com.keybudget.auth;

import java.time.Instant;

public interface RefreshTokenService {
    RefreshToken store(String jti, Long userId, Instant expiresAt, String familyId);
    RefreshToken validateAndRevoke(String jti);
    void revokeFamily(String familyId);
    void revokeAllForUser(Long userId);
    void purgeExpired();
}
