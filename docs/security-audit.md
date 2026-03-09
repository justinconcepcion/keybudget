# KeyBudget Backend Security Audit Report

**Date:** 2026-03-09
**Auditor:** Automated Security Review (claude-opus-4-6)
**Scope:** `backend/src/` — all Java source, configuration properties, and POM
**Spring Boot Version:** 3.3.5
**JJWT Version:** 0.12.6

---

## Executive Summary

The KeyBudget backend demonstrates solid security foundations: RSA-signed JWTs, AES-256-GCM credential encryption, stateless session management, proper CORS scoping, and secrets externalized via environment variables in production. No hardcoded credentials were found in tracked source files.

However, the audit identified **2 High**, **6 Medium**, and **4 Low/Informational** findings that should be addressed before production deployment.

**Verdict:** NOT approved for production deployment until all High findings are resolved.

---

## Summary Table

| # | Severity | Finding | Status |
|---|----------|---------|--------|
| 1 | HIGH | Missing JWT issuer validation on decoder | Open |
| 2 | HIGH | No refresh token revocation | Open |
| 3 | MEDIUM | Missing SameSite on refresh cookie | Open |
| 4 | MEDIUM | Secure flag depends on request scheme | Open |
| 5 | MEDIUM | Refresh token in response body breaks HttpOnly model | Open |
| 6 | MEDIUM | No rate limiting on auth/connect endpoints | Open |
| 7 | MEDIUM | No size validation on credentials map | Open |
| 8 | MEDIUM | No upper bound on `days` parameter | Open |
| 9 | LOW | Test properties contain real-format keys | Open |
| 10 | LOW | CSRF disabled globally (acceptable for JWT API) | Accepted |
| 11 | LOW | Provider error messages exposed to client | Open |
| 12 | INFO | Spring Boot 3.3.5 is past OSS support | Open |

---

## HIGH Findings

### FINDING 1 — Missing JWT Issuer Validation on Resource Server Decoder

- **Location:** `config/SecurityConfig.java`, lines 67-76
- **Issue:** The `NimbusJwtDecoder` is configured with only the RSA public key for signature verification. It does not validate the `iss` (issuer) claim. The `JwtServiceImpl` sets `issuer("keybudget-api")` when building tokens, but the decoder never checks that the `iss` claim matches.
- **Risk:** If the RSA private key is ever compromised or if another service shares the same keypair, tokens with arbitrary issuer values will be accepted.
- **Remediation:** Add a `JwtValidator` that checks the issuer claim:

```java
OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(),
        new JwtIssuerValidator("keybudget-api")
);
decoder.setJwtValidator(withIssuer);
```

### FINDING 2 — No Refresh Token Revocation / Rotation Enforcement

- **Location:** `auth/AuthController.java`, lines 25-35; `auth/JwtServiceImpl.java`, lines 55-62
- **Issue:** The refresh token flow issues a new refresh token on every `/api/v1/auth/refresh` call, but the old refresh token is never invalidated. The `/api/v1/auth/logout` endpoint is a no-op.
- **Risk:** A stolen refresh token can be used indefinitely (up to 7 days) to mint new access tokens, even after logout.
- **Remediation:** Implement server-side refresh token store with JTI tracking. On each refresh, invalidate the old JTI. On logout, delete all JTIs for the user.

---

## MEDIUM Findings

### FINDING 3 — Refresh Token Cookie Missing SameSite Attribute

- **Location:** `auth/OAuth2SuccessHandler.java`, lines 55-60
- **Issue:** The `jakarta.servlet.http.Cookie` API does not support `SameSite`. Without it, the cookie may be sent on cross-site requests.
- **Remediation:** Use `ResponseCookie` from Spring:

```java
ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true).secure(true).path("/api/v1/auth/refresh")
        .maxAge(Duration.ofDays(7)).sameSite("Strict").build();
response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
```

### FINDING 4 — Secure Flag Depends on Request Scheme

- **Location:** `auth/OAuth2SuccessHandler.java`, line 57
- **Issue:** `refreshCookie.setSecure(request.isSecure())` may return `false` behind TLS-terminating reverse proxies.
- **Remediation:** Always set `Secure=true` in production. Use profile-based approach for dev.

### FINDING 5 — Refresh Token in Response Body Breaks HttpOnly Model

- **Location:** `auth/AuthController.java`, line 35
- **Issue:** The `/api/v1/auth/refresh` endpoint returns the new refresh token in the JSON response body, making it accessible to JavaScript/XSS, defeating the HttpOnly cookie model.
- **Remediation:** Read refresh token from HttpOnly cookie (`@CookieValue`), set new refresh token as HttpOnly cookie. Remove `refreshToken` from `AuthResponse`.

### FINDING 6 — No Rate Limiting on Authentication Endpoints

- **Location:** `auth/AuthController.java`; `integration/IntegrationController.java`, line 31
- **Issue:** No rate limiting on `/api/v1/auth/refresh` (permitAll) or `/api/v1/integrations/connect`.
- **Remediation:** Add per-IP rate limiter (e.g., Bucket4j, Resilience4j) — 10 req/min on auth endpoints.

### FINDING 7 — No Input Size Validation on ConnectAccountRequest Credentials Map

- **Location:** `integration/dto/ConnectAccountRequest.java`, line 19
- **Issue:** `Map<String, String>` credentials has only `@NotNull` — no constraints on entry count or value length.
- **Remediation:** Add `@Size(min = 1, max = 10)` on the map, `@Size(max = 500)` on values.

### FINDING 8 — No Upper Bound on `days` Query Parameter

- **Location:** `integration/IntegrationController.java`, line 106
- **Issue:** `/net-worth/history?days=999999` would iterate ~2,700 years of dates.
- **Remediation:** Add `@Min(1) @Max(365)` validation.

---

## LOW / INFO Findings

### FINDING 9 — Test Properties Contain Real-Format RSA Keys (LOW)
Keys in `target/test-classes/application.properties`. Not a source leak (excluded by `.gitignore`), but could cause confusion.

### FINDING 10 — CSRF Disabled Globally (LOW, Accepted)
Standard for stateless JWT APIs. Note: if Finding 5 is implemented (cookie-based refresh), CSRF must be re-evaluated for that endpoint.

### FINDING 11 — Provider Error Messages Exposed to Client (LOW)
`ProviderException` messages include upstream HTTP status codes. Map to generic user-facing messages.

### FINDING 12 — Spring Boot 3.3.5 Past OSS Support (INFO)
Upgrade to Spring Boot 3.4.x or 3.5.x for security patches.

---

## Positive Findings (No Action Required)

| Area | Assessment |
|------|-----------|
| RSA JWT signing | Correct. No algorithm confusion risk. |
| JWT expiry enforcement | Correct. Both access (15min) and refresh (7d) enforced. |
| JWT token type separation | Correct. Access/refresh tokens carry `tokenType` claim. |
| AES-256-GCM encryption | Correct. 12-byte random IV, 128-bit GCM auth tag, `SecureRandom`. |
| No hardcoded secrets | Verified. Dev uses `REPLACE_ME`, prod uses `${ENV_VAR}`. |
| SQL/JPQL injection | Not found. All queries parameterized. |
| CORS configuration | Properly scoped to single origin, no wildcards. |
| Session management | Correct. `STATELESS` policy, no session-creating code. |
| Actuator exposure | Only `/actuator/health` exposed, `show-details=never`. |
| Error response hardening | Stacktrace, message, binding errors all suppressed. |
| Integration credential ownership | Verified via `resolveCredential()` userId check. |
| Credential logging | API keys and addresses masked in logs. |
| Mass assignment | Not vulnerable. DTOs are Java records. |
| OAuth2 scope | Limited to `openid,email,profile`. |

---

## Files Reviewed

- `config/SecurityConfig.java`
- `auth/OAuth2SuccessHandler.java`
- `auth/JwtService.java`, `auth/JwtServiceImpl.java`
- `auth/AuthController.java`
- `auth/dto/RefreshRequest.java`, `auth/dto/AuthResponse.java`
- `shared/encryption/AesEncryptionServiceImpl.java`
- `shared/GlobalExceptionHandler.java`
- `integration/IntegrationController.java`
- `integration/IntegrationOrchestrationServiceImpl.java`
- `integration/model/IntegrationCredential.java`, `integration/model/FinancialAccount.java`
- `integration/dto/ConnectAccountRequest.java`
- `integration/provider/coinbase/CoinbaseProvider.java`
- `integration/provider/bitcoin/BitcoinWalletProvider.java`
- `integration/provider/m1finance/M1FinanceProvider.java`
- `integration/provider/marcus/MarcusProvider.java`
- `integration/scheduler/IntegrationSyncScheduler.java`
- `config/WebClientConfig.java`
- `user/User.java`, `user/UserController.java`, `user/UserServiceImpl.java`
- `application.properties`, `application-dev.properties`, `application-prod.properties`
- `pom.xml`, `.gitignore`
