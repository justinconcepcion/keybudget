package com.keybudget.auth;

import com.keybudget.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceImplTest {

    // Same test keys from test/resources/application.properties
    private static final String PRIVATE_KEY_BASE64 =
            "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQC8oNmy7Ch3t9/G2QY0AL7t7Q5zE+gVMcwex6OzuCgFa26iDLcY+/mPlUzJgvV24kuG4B43MCKgqpK9DgxDLrNXxNsOlbpJNHfvXWJzBNKR6c8J2K7P9v0BAibKLVxe14trgGV3HGCr/WMz+RnKbOMJ9f3cbqTd3rYYxV3dMl3zda1E79/rwprrUZXaLaqm8I8HBUNV8H18K2kG2BGSI9ihwr85Rw7UIjKBPPQCImQcusjaSswe4LGUs/jILZAqVRE21wLUW27aZPnTqF6y4VHDqZVZxz51AANoDsUFAjZJlBInHZIQBRv7SiT94N6EtxzGfvNQn11g1R/acCwf1ow9AgMBAAECggEAAVP+ETRE7mfsp9xUL8CMICGYuTcI1TBNmmKLIGzYnJ/S2koasjDndCf8DZbEKTXTA5PcuQAm/sveQeQK3Q9uWX8Vw6BKlmJc0nvcI7E+5m8892BqONApnDQskwKUBK8QskTWy6ZD7wy9cPiJ7T+SryAagF/a7NiW87OhYUHSS3TBkm9AB2dL5lFmGrIrrkoYqwfH5V9UKYi167wd+0ITgqufS0RX/xSzjvwkq9eTpmE9Chp1c7Es38B6hMI1J/vgHTD/jLJoSdF1j6+oJGNP+r7eYQQzoTWkv2YWKYkRGfIMkctqQCkOdJkzUeAc/mtw8autxFfi0KsEkGr95SRUYQKBgQDEBB392IanW3xOCWfL9LlKvghubxrnCA4KJprfIybegoCZEZYIEQ+TlKXvvVrdJzRII69SFEAHCWgfFft6WNVVikN+011fnA/yX4Pk1yFYTLVP3IPleWlrUZLwqERNMXUEFp5q4XMiGBCS02CmXZRdBKGPDO2PuYNs90OKbbQ50QKBgQD2WfoIjEtfdgk72nVwEvdJIkqZV8bGJkFdXwsuv6j69yWwfUsrEZ05JU6osu9F9h+m5ZlpbMrZaLjT/4AxHm5jQi25sSm/mRagVFfCmpr85ASuAImwMKQsXEtvrHMRKXVV3g5bZs3GJoI9HHI8s8/T3wjGLFOtkzekhFRGCzharQKBgQCWba5ynFQiZbRM0y0EOihlMteXoM6dlPHpWl0qd3ih9LcqQXPJdwZkNQTvqnrsE+Uso64tqHrGq7JrgNxAtaHrBrPrxdv3vvYCBMlcurjNYfkXc90JI5cClFTXOdtI/naWD6G91o0cEinN9EhNBK4SgLZX6Qz7atxwqIX921kfsQKBgQCHDlL2Rru3rQVo695AQBWT4ZnWMXoG/cgOAWInEen5FG90L2x7Qx/XyW0zLU3iJAm8PNl7I6qdwDy79LH5u7426nwY9lh18t6lrTejt5DKndM29ZKGplQLAdpVccxvlzP9jEyArX6YaFo0WJkEUGvassajpn+FhFUX+3gcImCVzQKBgQC7aCDFp8bIPEsIBI3As3hu6S/ZPKgvyDjWf133wpfphhYJl1mS77uDYkx2RFBjzsX5InEDzgFlZbsRHRcc+PFAj/CDNNtF6o8FDGbA4YtOfUohtKls2QGDgeveZdzsK2pb5I2tpxdhx9AQbiRYjoT/rT5BMjcNacH0yA29RLtLjQ==";
    private static final String PUBLIC_KEY_BASE64 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvKDZsuwod7ffxtkGNAC+7e0OcxPoFTHMHsejs7goBWtuogy3GPv5j5VMyYL1duJLhuAeNzAioKqSvQ4MQy6zV8TbDpW6STR3711icwTSkenPCdiuz/b9AQImyi1cXteLa4Bldxxgq/1jM/kZymzjCfX93G6k3d62GMVd3TJd83WtRO/f68Ka61GV2i2qpvCPBwVDVfB9fCtpBtgRkiPYocK/OUcO1CIygTz0AiJkHLrI2krMHuCxlLP4yC2QKlURNtcC1Ftu2mT506hesuFRw6mVWcc+dQADaA7FBQI2SZQSJx2SEAUb+0ok/eDehLccxn7zUJ9dYNUf2nAsH9aMPQIDAQAB";
    private static final String ISSUER = "keybudget-api";
    private static final String AUDIENCE = "keybudget";

    private static PrivateKey privateKey;
    private static PublicKey publicKey;

    private JwtServiceImpl jwtService;

    @BeforeAll
    static void initKeys() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        privateKey = kf.generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY_BASE64)));
        publicKey = kf.generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY_BASE64)));
    }

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtServiceImpl(PRIVATE_KEY_BASE64, PUBLIC_KEY_BASE64, ISSUER, AUDIENCE);
    }

    private User buildUser(Long id) {
        User user = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user id via reflection", e);
        }
        // Set email via reflection since there's no setter
        try {
            Field emailField = User.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(user, "test@example.com");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set user email via reflection", e);
        }
        return user;
    }

    // -------------------------------------------------------------------------
    // issueAccessToken()
    // -------------------------------------------------------------------------

    @Test
    void issueAccessToken_givenUser_containsCorrectIssuer() {
        User user = buildUser(1L);
        String token = jwtService.issueAccessToken(user);

        Claims claims = parseClaims(token);
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    void issueAccessToken_givenUser_containsCorrectAudience() {
        User user = buildUser(1L);
        String token = jwtService.issueAccessToken(user);

        Claims claims = parseClaims(token);
        assertThat(claims.getAudience()).contains(AUDIENCE);
    }

    @Test
    void issueAccessToken_givenUser_containsAccessTokenType() {
        User user = buildUser(1L);
        String token = jwtService.issueAccessToken(user);

        Claims claims = parseClaims(token);
        assertThat(claims.get("tokenType", String.class)).isEqualTo("access");
    }

    @Test
    void issueAccessToken_givenUser_containsUserIdAndEmail() {
        User user = buildUser(42L);
        String token = jwtService.issueAccessToken(user);

        Claims claims = parseClaims(token);
        assertThat(claims.get("userId", Long.class)).isEqualTo(42L);
        assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
    }

    // -------------------------------------------------------------------------
    // issueRefreshToken()
    // -------------------------------------------------------------------------

    @Test
    void issueRefreshToken_givenUser_containsRefreshTokenType() {
        User user = buildUser(1L);
        String token = jwtService.issueRefreshToken(user);

        Claims claims = parseClaims(token);
        assertThat(claims.get("tokenType", String.class)).isEqualTo("refresh");
    }

    // -------------------------------------------------------------------------
    // extractUserId()
    // -------------------------------------------------------------------------

    @Test
    void extractUserId_givenValidToken_returnsUserId() {
        User user = buildUser(99L);
        String token = jwtService.issueAccessToken(user);

        Long userId = jwtService.extractUserId(token);
        assertThat(userId).isEqualTo(99L);
    }

    // -------------------------------------------------------------------------
    // isValidRefreshToken()
    // -------------------------------------------------------------------------

    @Test
    void isValidRefreshToken_givenRefreshToken_returnsTrue() {
        User user = buildUser(1L);
        String token = jwtService.issueRefreshToken(user);

        assertThat(jwtService.isValidRefreshToken(token)).isTrue();
    }

    @Test
    void isValidRefreshToken_givenAccessToken_returnsFalse() {
        User user = buildUser(1L);
        String token = jwtService.issueAccessToken(user);

        assertThat(jwtService.isValidRefreshToken(token)).isFalse();
    }

    @Test
    void isValidRefreshToken_givenGarbageToken_returnsFalse() {
        assertThat(jwtService.isValidRefreshToken("not-a-valid-jwt")).isFalse();
    }

    // -------------------------------------------------------------------------
    // extractJti()
    // -------------------------------------------------------------------------

    @Test
    void extractJti_givenValidToken_returnsNonNullJti() {
        User user = buildUser(1L);
        String token = jwtService.issueAccessToken(user);

        String jti = jwtService.extractJti(token);
        assertThat(jti).isNotNull().isNotBlank();
    }

    // -------------------------------------------------------------------------
    // parseClaims() — wrong issuer
    // -------------------------------------------------------------------------

    @Test
    void parseClaims_givenWrongIssuer_throwsJwtException() {
        User user = buildUser(1L);
        // Build a token with wrong issuer
        String wrongIssuerToken = Jwts.builder()
                .subject("1")
                .claim("userId", 1L)
                .claim("tokenType", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .issuer("wrong-issuer")
                .audience().add(AUDIENCE).and()
                .signWith(privateKey)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(wrongIssuerToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void parseClaims_givenWrongAudience_throwsJwtException() {
        User user = buildUser(1L);
        String wrongAudienceToken = Jwts.builder()
                .subject("1")
                .claim("userId", 1L)
                .claim("tokenType", "access")
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .issuer(ISSUER)
                .audience().add("wrong-audience").and()
                .signWith(privateKey)
                .compact();

        assertThatThrownBy(() -> jwtService.extractUserId(wrongAudienceToken))
                .isInstanceOf(JwtException.class);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
