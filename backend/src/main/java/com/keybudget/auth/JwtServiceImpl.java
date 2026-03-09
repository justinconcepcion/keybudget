package com.keybudget.auth;

import com.keybudget.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 15 * 60;         // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7 * 24 * 3600;  // 7 days

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtServiceImpl(
            @Value("${app.jwt.private-key}") String privateKeyBase64,
            @Value("${app.jwt.public-key}") String publicKeyBase64) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = kf.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64)));
        this.publicKey = kf.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));
    }

    @Override
    public String issueAccessToken(User user) {
        return buildToken(user, ACCESS_TOKEN_EXPIRY_SECONDS, "access");
    }

    @Override
    public String issueRefreshToken(User user) {
        return buildToken(user, REFRESH_TOKEN_EXPIRY_SECONDS, "refresh");
    }

    @Override
    public Long extractUserId(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    @Override
    public boolean isValidRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private String buildToken(User user, long expirySeconds, String tokenType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("email", user.getEmail())
                .claim("tokenType", tokenType)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .issuer("keybudget-api")
                .signWith(privateKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
