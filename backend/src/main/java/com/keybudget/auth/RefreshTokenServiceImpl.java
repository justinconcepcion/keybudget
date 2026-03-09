package com.keybudget.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public RefreshToken store(String jti, Long userId, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setJti(jti);
        token.setUserId(userId);
        token.setExpiresAt(expiresAt);
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String jti) {
        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
        if (token.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        return token;
    }

    @Override
    @Transactional
    public void revoke(String jti) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByJti(jti);
        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }
}
