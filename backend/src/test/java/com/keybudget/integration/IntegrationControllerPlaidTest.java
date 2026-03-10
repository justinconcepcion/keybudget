package com.keybudget.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.dto.AccountResponse;
import com.keybudget.integration.dto.ConnectAccountRequest;
import com.keybudget.integration.dto.PlaidExchangeRequest;
import com.keybudget.integration.exception.ProviderAuthException;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.exception.ProviderRateLimitException;
import com.keybudget.integration.provider.plaid.PlaidService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for the Plaid-specific endpoints added to {@link IntegrationController}:
 * <ul>
 *   <li>{@code POST /api/v1/integrations/plaid/link-token}</li>
 *   <li>{@code POST /api/v1/integrations/plaid/exchange}</li>
 * </ul>
 *
 * Covers 200/201, 400, 401, 429, 500, 502, and unauthenticated 401 scenarios.
 * {@link IntegrationOrchestrationService} and {@link PlaidService} are both mocked;
 * no real Plaid API calls are made.
 *
 * <p>Valid Plaid public tokens must match the pattern:
 * {@code ^public-(sandbox|development|production)-[a-f0-9\-]{36}$}
 * — a UUID-like 36-character hex-plus-dash string after the environment prefix.
 */
@WebMvcTest(IntegrationController.class)
class IntegrationControllerPlaidTest {

    private static final long USER_ID = 42L;

    /**
     * A valid public token matching {@code ^public-sandbox-[a-f0-9\-]{36}$}.
     * Uses a real UUID-format hex string for the suffix.
     */
    private static final String VALID_PUBLIC_TOKEN =
            "public-sandbox-a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private IntegrationOrchestrationService orchestrationService;
    @MockBean private PlaidService plaidService;

    // =========================================================================
    // POST /api/v1/integrations/plaid/link-token
    // =========================================================================

    @Nested
    class CreatePlaidLinkToken {

        @Test
        void createPlaidLinkToken_givenM1Finance_200() throws Exception {
            when(plaidService.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .thenReturn(new PlaidService.PlaidLinkTokenResult(
                            "link-sandbox-abc123", "2026-03-09T12:30:00Z"));

            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "M1_FINANCE")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linkToken").value("link-sandbox-abc123"))
                    .andExpect(jsonPath("$.expiration").value("2026-03-09T12:30:00Z"));
        }

        @Test
        void createPlaidLinkToken_givenMarcus_200() throws Exception {
            when(plaidService.createLinkToken(USER_ID, ProviderType.MARCUS))
                    .thenReturn(new PlaidService.PlaidLinkTokenResult(
                            "link-sandbox-marcus456", "2026-03-09T12:30:00Z"));

            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "MARCUS")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.linkToken").value("link-sandbox-marcus456"));
        }

        @Test
        void createPlaidLinkToken_givenNonPlaidProvider_400() throws Exception {
            // The service layer throws IllegalArgumentException for non-Plaid providers;
            // GlobalExceptionHandler maps that to 400 BAD_REQUEST.
            when(plaidService.createLinkToken(USER_ID, ProviderType.BITCOIN_WALLET))
                    .thenThrow(new IllegalArgumentException(
                            "Provider BITCOIN_WALLET is not a Plaid-backed provider"));

            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "BITCOIN_WALLET")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void createPlaidLinkToken_givenMissingProvider_500() throws Exception {
            // Spring's MissingServletRequestParameterException is not handled by
            // GlobalExceptionHandler, so it falls through to the generic 500 handler.
            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void createPlaidLinkToken_givenInvalidProviderValue_500() throws Exception {
            // MethodArgumentTypeMismatchException (enum conversion failure) is not handled
            // by GlobalExceptionHandler — falls through to generic 500 handler.
            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "UNKNOWN_BANK")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        void createPlaidLinkToken_givenNoJwt_401() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "M1_FINANCE")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void createPlaidLinkToken_givenPlaidCredentialRejected_401() throws Exception {
            when(plaidService.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .thenThrow(new ProviderAuthException(
                            ProviderType.M1_FINANCE,
                            "Plaid rejected credentials — check PLAID_CLIENT_ID and PLAID_SECRET"));

            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "M1_FINANCE")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("PROVIDER_AUTH_ERROR"));
        }

        @Test
        void createPlaidLinkToken_givenPlaidRateLimit_429() throws Exception {
            when(plaidService.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .thenThrow(new ProviderRateLimitException(
                            ProviderType.M1_FINANCE,
                            "Plaid API rate limit exceeded on /link/token/create"));

            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "M1_FINANCE")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("PROVIDER_RATE_LIMIT"));
        }

        @Test
        void createPlaidLinkToken_givenPlaidServerError_502() throws Exception {
            when(plaidService.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .thenThrow(new ProviderException(
                            ProviderType.M1_FINANCE,
                            "Plaid returned server error on /link/token/create: HTTP 503"));

            mockMvc.perform(post("/api/v1/integrations/plaid/link-token")
                            .param("provider", "M1_FINANCE")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("PROVIDER_ERROR"));
        }
    }

    // =========================================================================
    // POST /api/v1/integrations/plaid/exchange
    // =========================================================================

    @Nested
    class ExchangePlaidPublicToken {

        private AccountResponse buildAccountResponse(ProviderType provider) {
            return new AccountResponse(
                    1L, 10L, provider, AccountType.BROKERAGE,
                    "M1 Finance Account", "USD",
                    new BigDecimal("10000.00"), new BigDecimal("10000.00"),
                    Instant.now(), true
            );
        }

        @Test
        void exchangePlaidPublicToken_givenValidM1Request_201() throws Exception {
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            when(plaidService.exchangePublicToken(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .thenReturn(new PlaidService.PlaidAccessTokenResult(
                            "access-sandbox-perm456", "item-sandbox-789"));

            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenReturn(List.of(buildAccountResponse(ProviderType.M1_FINANCE)));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].providerType").value("M1_FINANCE"))
                    .andExpect(jsonPath("$[0].active").value(true));
        }

        @Test
        void exchangePlaidPublicToken_givenValidMarcusRequest_201() throws Exception {
            String marcusToken = "public-sandbox-b2c3d4e5-f6a7-8901-bcde-f12345678901";
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(marcusToken, ProviderType.MARCUS);

            when(plaidService.exchangePublicToken(marcusToken, ProviderType.MARCUS))
                    .thenReturn(new PlaidService.PlaidAccessTokenResult(
                            "access-sandbox-marcus-perm", "item-marcus-777"));

            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenReturn(List.of(buildAccountResponse(ProviderType.MARCUS)));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].providerType").value("MARCUS"));
        }

        @Test
        void exchangePlaidPublicToken_givenMissingPublicToken_400() throws Exception {
            // publicToken is @NotBlank — omitting it triggers MethodArgumentNotValidException
            String bodyWithoutPublicToken = objectMapper.writeValueAsString(
                    Map.of("provider", "M1_FINANCE"));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithoutPublicToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void exchangePlaidPublicToken_givenBlankPublicToken_400() throws Exception {
            // Blank value fails @NotBlank before @Pattern is evaluated
            String body = "{\"publicToken\":\"   \",\"provider\":\"M1_FINANCE\"}";

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void exchangePlaidPublicToken_givenInvalidPublicTokenFormat_400() throws Exception {
            // Fails the @Pattern constraint — not a valid Plaid token format
            String body = "{\"publicToken\":\"not-a-valid-plaid-token\",\"provider\":\"M1_FINANCE\"}";

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void exchangePlaidPublicToken_givenMissingProvider_400() throws Exception {
            // provider is @NotNull — omitting it triggers MethodArgumentNotValidException
            String bodyWithoutProvider = objectMapper.writeValueAsString(
                    Map.of("publicToken", VALID_PUBLIC_TOKEN));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithoutProvider))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void exchangePlaidPublicToken_givenNoJwt_401() throws Exception {
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void exchangePlaidPublicToken_givenExpiredPublicToken_401() throws Exception {
            // Use a token format that passes validation but Plaid rejects as expired
            String expiredToken = "public-sandbox-dead0000-0000-0000-0000-000000000000";
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(expiredToken, ProviderType.M1_FINANCE);

            when(plaidService.exchangePublicToken(anyString(), eq(ProviderType.M1_FINANCE)))
                    .thenThrow(new ProviderAuthException(
                            ProviderType.M1_FINANCE,
                            "Plaid rejected the public token — it may have expired or already been exchanged"));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("PROVIDER_AUTH_ERROR"));
        }

        @Test
        void exchangePlaidPublicToken_givenPlaidRateLimit_429() throws Exception {
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            when(plaidService.exchangePublicToken(anyString(), eq(ProviderType.M1_FINANCE)))
                    .thenThrow(new ProviderRateLimitException(
                            ProviderType.M1_FINANCE,
                            "Plaid API rate limit exceeded on /item/public_token/exchange"));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error").value("PROVIDER_RATE_LIMIT"));
        }

        @Test
        void exchangePlaidPublicToken_givenPlaidServerError_502() throws Exception {
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            when(plaidService.exchangePublicToken(anyString(), eq(ProviderType.M1_FINANCE)))
                    .thenThrow(new ProviderException(
                            ProviderType.M1_FINANCE,
                            "Plaid returned server error on /item/public_token/exchange: HTTP 503"));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("PROVIDER_ERROR"));
        }

        @Test
        void exchangePlaidPublicToken_givenAlreadyConnected_400() throws Exception {
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            when(plaidService.exchangePublicToken(anyString(), eq(ProviderType.M1_FINANCE)))
                    .thenReturn(new PlaidService.PlaidAccessTokenResult(
                            "access-sandbox-perm456", "item-sandbox-789"));

            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenThrow(new IllegalArgumentException("Provider already connected: M1_FINANCE"));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        void exchangePlaidPublicToken_givenOrchestrationServiceThrows_500() throws Exception {
            PlaidExchangeRequest request =
                    new PlaidExchangeRequest(VALID_PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            when(plaidService.exchangePublicToken(anyString(), eq(ProviderType.M1_FINANCE)))
                    .thenReturn(new PlaidService.PlaidAccessTokenResult(
                            "access-sandbox-perm456", "item-sandbox-789"));

            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenThrow(new RuntimeException("Database connection lost"));

            mockMvc.perform(post("/api/v1/integrations/plaid/exchange")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
        }
    }
}
