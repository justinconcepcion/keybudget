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

    @Test
    void store_givenValidArgs_savesAndReturnsToken() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        RefreshToken saved = new RefreshToken();
        saved.setJti("jti");
        saved.setUserId(1L);
        saved.setExpiresAt(expiresAt);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

        RefreshToken result = refreshTokenService.store("jti", 1L, expiresAt);

        assertThat(result.getJti()).isEqualTo("jti");
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void validate_givenActiveJti_returnsToken() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validate("jti");

        assertThat(result).isSameAs(token);
    }

    @Test
    void validate_givenUnknownJti_throwsIllegalArgument() {
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("jti"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token not found");
    }

    @Test
    void validate_givenRevokedJti_throwsIllegalArgument() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        token.setRevokedAt(Instant.now());
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validate("jti"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Refresh token has been revoked");
    }

    @Test
    void revoke_givenActiveJti_setsRevokedAt() {
        RefreshToken token = new RefreshToken();
        token.setJti("jti");
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.of(token));

        refreshTokenService.revoke("jti");

        verify(refreshTokenRepository).save(token);
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_givenUnknownJti_doesNotThrow() {
        when(refreshTokenRepository.findByJti("jti")).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> refreshTokenService.revoke("jti"));
    }
}
