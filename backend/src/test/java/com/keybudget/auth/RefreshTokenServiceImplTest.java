package com.keybudget.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository);
    }

    // -------------------------------------------------------------------------
    // store()
    // -------------------------------------------------------------------------

    @Test
    void store_givenValidArgs_savesAndReturnsToken() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        RefreshToken saved = new RefreshToken();
        saved.setJti("jti");
        saved.setUserId(1L);
        saved.setExpiresAt(expiresAt);
        saved.setFamilyId("family-1");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

        RefreshToken result = refreshTokenService.store("jti", 1L, expiresAt, "family-1");

        assertThat(result.getJti()).isEqualTo("jti");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.getFamilyId()).isEqualTo("family-1");
    }

    // -------------------------------------------------------------------------
    // validateAndRevoke()
    // -------------------------------------------------------------------------

    @Test
    void validateAndRevoke_givenActiveToken_revokesAndReturnsToken() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        token.setFamilyId("family-1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.revokeIfActive(eq("jti"), any(Instant.class))).thenReturn(1);

        RefreshToken result = refreshTokenService.validateAndRevoke("jti");

        assertThat(result).isSameAs(token);
        verify(refreshTokenRepository).revokeIfActive(eq("jti"), any(Instant.class));
    }

    @Test
    void validateAndRevoke_givenUnknownJti_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke("jti"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void validateAndRevoke_givenExpiredToken_throwsInvalidRefreshTokenException() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        token.setExpiresAt(Instant.now().minusSeconds(3600));
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke("jti"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateAndRevoke_givenRevokedToken_revokesEntireFamily() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        token.setFamilyId("family-1");
        token.setUserId(1L);
        token.setRevokedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke("jti"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("revoked");

        verify(refreshTokenRepository).revokeAllActiveByFamilyId(eq("family-1"), any(Instant.class));
    }

    @Test
    void validateAndRevoke_givenConcurrentUse_revokesEntireFamily() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        token.setFamilyId("family-1");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.revokeIfActive(eq("jti"), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.validateAndRevoke("jti"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("already consumed");

        verify(refreshTokenRepository).revokeAllActiveByFamilyId(eq("family-1"), any(Instant.class));
    }

    // -------------------------------------------------------------------------
    // revokeFamily()
    // -------------------------------------------------------------------------

    @Test
    void revokeFamily_givenFamilyId_revokesAllActiveInFamily() {
        when(refreshTokenRepository.revokeAllActiveByFamilyId(eq("family-1"), any(Instant.class))).thenReturn(3);

        refreshTokenService.revokeFamily("family-1");

        verify(refreshTokenRepository).revokeAllActiveByFamilyId(eq("family-1"), any(Instant.class));
    }

    // -------------------------------------------------------------------------
    // revokeAllForUser()
    // -------------------------------------------------------------------------

    @Test
    void revokeAllForUser_givenUserId_revokesAllActive() {
        when(refreshTokenRepository.revokeAllActiveByUserId(eq(1L), any(Instant.class))).thenReturn(2);

        refreshTokenService.revokeAllForUser(1L);

        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(1L), any(Instant.class));
    }

    // -------------------------------------------------------------------------
    // purgeExpired()
    // -------------------------------------------------------------------------

    @Test
    void purgeExpired_givenOldTokens_deletesRevokedAndExpired() {
        when(refreshTokenRepository.deleteRevokedBefore(any(Instant.class))).thenReturn(5);
        when(refreshTokenRepository.deleteExpiredBefore(any(Instant.class))).thenReturn(3);

        refreshTokenService.purgeExpired();

        verify(refreshTokenRepository).deleteRevokedBefore(any(Instant.class));
        verify(refreshTokenRepository).deleteExpiredBefore(any(Instant.class));
    }
}
