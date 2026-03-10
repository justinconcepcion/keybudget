package com.keybudget.integration.provider.plaid;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderAuthException;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.exception.ProviderRateLimitException;
import com.keybudget.integration.provider.plaid.config.PlaidConfig;
import com.keybudget.integration.provider.plaid.dto.PlaidLinkTokenCreateResponse;
import com.keybudget.integration.provider.plaid.dto.PlaidPublicTokenExchangeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PlaidServiceImpl}.
 *
 * <p>WebClient is mocked via its builder/chain so no real HTTP calls are made.
 * The builder's {@code clone()} is stubbed to return itself, mirroring the pattern
 * used in {@link com.keybudget.integration.provider.bitcoin.BitcoinWalletProviderTest}.
 */
@ExtendWith(MockitoExtension.class)
class PlaidServiceImplTest {

    private static final long   USER_ID      = 42L;
    private static final String PUBLIC_TOKEN = "public-sandbox-abc123";
    private static final String LINK_TOKEN   = "link-sandbox-xyz789";
    private static final String ACCESS_TOKEN = "access-sandbox-perm456";
    private static final String ITEM_ID      = "item-sandbox-item789";
    private static final String EXPIRATION   = "2026-03-09T12:30:00Z";

    // --- WebClient mock hierarchy --------------------------------------------
    @Mock private WebClient.Builder         webClientBuilder;
    @Mock private WebClient                 plaidWebClient;
    @Mock private WebClient.RequestBodyUriSpec  requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec     requestBodySpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec        responseSpec;

    private PlaidServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(plaidWebClient);

        PlaidConfig config = new PlaidConfig();
        config.setClientId("test-client-id");
        config.setSecret("test-secret");
        config.setBaseUrl("https://sandbox.plaid.com");

        service = new PlaidServiceImpl(webClientBuilder, config);
    }

    // -------------------------------------------------------------------------
    // createLinkToken()
    // -------------------------------------------------------------------------

    @Nested
    class CreateLinkToken {

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenM1Finance_returnsLinkTokenResult() {
            stubPlaidPost(new PlaidLinkTokenCreateResponse(LINK_TOKEN, EXPIRATION, "req-1"));

            PlaidService.PlaidLinkTokenResult result =
                    service.createLinkToken(USER_ID, ProviderType.M1_FINANCE);

            assertThat(result.linkToken()).isEqualTo(LINK_TOKEN);
            assertThat(result.expiration()).isEqualTo(EXPIRATION);
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenMarcus_returnsLinkTokenResult() {
            stubPlaidPost(new PlaidLinkTokenCreateResponse(LINK_TOKEN, EXPIRATION, "req-2"));

            PlaidService.PlaidLinkTokenResult result =
                    service.createLinkToken(USER_ID, ProviderType.MARCUS);

            assertThat(result.linkToken()).isEqualTo(LINK_TOKEN);
            assertThat(result.expiration()).isEqualTo(EXPIRATION);
        }

        @Test
        void createLinkToken_givenNonPlaidProvider_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.BITCOIN_WALLET))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a Plaid-backed provider");
        }

        @Test
        void createLinkToken_givenCoinbaseProvider_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.COINBASE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a Plaid-backed provider");
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenPlaidCredentialRejected_throwsProviderAuthException() {
            // Simulate what the real onStatus(401/403) handler emits: ProviderAuthException.
            // Throwing it directly from bodyToMono bypasses wrapNonProviderException because
            // ProviderAuthException is already a ProviderException subclass.
            stubPlaidPostError(new ProviderAuthException(
                    ProviderType.M1_FINANCE,
                    "Plaid rejected credentials — check PLAID_CLIENT_ID and PLAID_SECRET"));

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderAuthException.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenPlaidRateLimit_throwsProviderRateLimitException() {
            // Simulate what the real onStatus(429) handler emits: ProviderRateLimitException.
            stubPlaidPostError(new ProviderRateLimitException(
                    ProviderType.M1_FINANCE,
                    "Plaid API rate limit exceeded on /link/token/create"));

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderRateLimitException.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenPlaid4xxClientError_throwsProviderException() {
            // Simulate what the real onStatus(4xx) handler emits for non-401/429 codes.
            stubPlaidPostError(new ProviderException(
                    ProviderType.M1_FINANCE,
                    "Plaid returned client error on /link/token/create: HTTP 400"));

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("client error");
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenPlaid5xxServerError_throwsProviderException() {
            // Simulate what the real onStatus(5xx) handler emits.
            stubPlaidPostError(new ProviderException(
                    ProviderType.M1_FINANCE,
                    "Plaid returned server error on /link/token/create: HTTP 503"));

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("server error");
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenNetworkFailure_throwsProviderException() {
            // Non-ProviderException throwables are caught by wrapNonProviderException
            // and wrapped into a ProviderException with "Failed to reach Plaid API".
            stubPlaidPostError(new java.net.ConnectException("Connection refused"));

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("Failed to reach Plaid API");
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenNullLinkTokenInResponse_throwsProviderException() {
            // Plaid returns HTTP 200 but with a null link_token field
            stubPlaidPost(new PlaidLinkTokenCreateResponse(null, EXPIRATION, "req-null"));

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("empty link token");
        }

        @Test
        @SuppressWarnings("unchecked")
        void createLinkToken_givenNullResponse_throwsProviderException() {
            stubPlaidPostNull(PlaidLinkTokenCreateResponse.class);

            assertThatThrownBy(() -> service.createLinkToken(USER_ID, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("empty link token");
        }
    }

    // -------------------------------------------------------------------------
    // exchangePublicToken()
    // -------------------------------------------------------------------------

    @Nested
    class ExchangePublicToken {

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenValidPublicToken_returnsAccessTokenResult() {
            stubPlaidPost(new PlaidPublicTokenExchangeResponse(ACCESS_TOKEN, ITEM_ID, "req-3"));

            PlaidService.PlaidAccessTokenResult result =
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.itemId()).isEqualTo(ITEM_ID);
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenMarcusProvider_returnsAccessTokenResult() {
            stubPlaidPost(new PlaidPublicTokenExchangeResponse(ACCESS_TOKEN, ITEM_ID, "req-4"));

            PlaidService.PlaidAccessTokenResult result =
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.MARCUS);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.itemId()).isEqualTo(ITEM_ID);
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenExpiredPublicToken_throwsProviderAuthException() {
            // Simulate what the real onStatus(401) handler emits: ProviderAuthException.
            stubPlaidPostError(new ProviderAuthException(
                    ProviderType.M1_FINANCE,
                    "Plaid rejected the public token — it may have expired or already been exchanged"));

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderAuthException.class)
                    .hasMessageContaining("public token");
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenPlaidRateLimit_throwsProviderRateLimitException() {
            // Simulate what the real onStatus(429) handler emits: ProviderRateLimitException.
            stubPlaidPostError(new ProviderRateLimitException(
                    ProviderType.M1_FINANCE,
                    "Plaid API rate limit exceeded on /item/public_token/exchange"));

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderRateLimitException.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenPlaid4xxClientError_throwsProviderException() {
            // Simulate what the real onStatus(4xx) handler emits for non-401/429 codes.
            stubPlaidPostError(new ProviderException(
                    ProviderType.M1_FINANCE,
                    "Plaid returned client error on /item/public_token/exchange: HTTP 400"));

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("client error");
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenPlaid5xxServerError_throwsProviderException() {
            // Simulate what the real onStatus(5xx) handler emits.
            stubPlaidPostError(new ProviderException(
                    ProviderType.M1_FINANCE,
                    "Plaid returned server error on /item/public_token/exchange: HTTP 500"));

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("server error");
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenNetworkFailure_throwsProviderException() {
            // Non-ProviderException throwables are caught by wrapNonProviderException
            // and wrapped into a ProviderException with "Failed to reach Plaid API".
            stubPlaidPostError(new java.net.ConnectException("Connection refused"));

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("Failed to reach Plaid API");
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenNullAccessTokenInResponse_throwsProviderException() {
            stubPlaidPost(new PlaidPublicTokenExchangeResponse(null, ITEM_ID, "req-null"));

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("empty token exchange");
        }

        @Test
        @SuppressWarnings("unchecked")
        void exchangePublicToken_givenNullResponse_throwsProviderException() {
            stubPlaidPostNull(PlaidPublicTokenExchangeResponse.class);

            assertThatThrownBy(() ->
                    service.exchangePublicToken(PUBLIC_TOKEN, ProviderType.M1_FINANCE))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("empty token exchange");
        }
    }

    // -------------------------------------------------------------------------
    // Stub helpers
    // -------------------------------------------------------------------------

    /**
     * Stubs the POST chain to return a successful Mono wrapping the given response object.
     * Works for both {@link PlaidLinkTokenCreateResponse} and {@link PlaidPublicTokenExchangeResponse}
     * because the chain topology is identical for both Plaid endpoints.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void stubPlaidPost(T responseBody) {
        when(plaidWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono((Class<T>) responseBody.getClass()))
                .thenReturn(Mono.just(responseBody));
    }

    /**
     * Stubs the POST chain to return a Mono that errors with the given exception.
     * Simulates HTTP errors or network failures from the Plaid API.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPlaidPostError(Exception error) {
        when(plaidWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        // Both response types share the same error Mono — use Object.class as the widest type
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(error));
    }

    /**
     * Stubs the POST chain to return {@code Mono.empty()} (null after {@code .block()}),
     * exercising the null-guard branches in both service methods.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubPlaidPostNull(Class<?> responseType) {
        when(plaidWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());
    }
}
