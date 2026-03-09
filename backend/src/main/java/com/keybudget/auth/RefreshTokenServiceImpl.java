package com.keybudget.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public RefreshToken store(String jti, Long userId, Instant expiresAt, String familyId) {
        RefreshToken token = new RefreshToken();
        token.setJti(jti);
        token.setUserId(userId);
        token.setExpiresAt(expiresAt);
        token.setFamilyId(familyId);
        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public RefreshToken validateAndRevoke(String jti) {
        RefreshToken token = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for familyId={}, userId={}", token.getFamilyId(), token.getUserId());
            refreshTokenRepository.revokeAllActiveByFamilyId(token.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }

        int updated = refreshTokenRepository.revokeIfActive(jti);
        if (updated == 0) {
            log.warn("Concurrent refresh token use detected for jti={}", jti);
            refreshTokenRepository.revokeAllActiveByFamilyId(token.getFamilyId());
            throw new InvalidRefreshTokenException("Refresh token already consumed");
        }

        return token;
    }

    @Override
    @Transactional
    public void revokeFamily(String familyId) {
        int revoked = refreshTokenRepository.revokeAllActiveByFamilyId(familyId);
        log.info("Revoked {} tokens in familyId={}", revoked, familyId);
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        int revoked = refreshTokenRepository.revokeAllActiveByUserId(userId);
        log.info("Revoked {} tokens for userId={}", revoked, userId);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        int deletedRevoked = refreshTokenRepository.deleteRevokedBefore(thirtyDaysAgo);
        int deletedExpired = refreshTokenRepository.deleteExpiredBefore(thirtyDaysAgo);
        log.info("Purged {} revoked and {} expired refresh tokens", deletedRevoked, deletedExpired);
    }
}
