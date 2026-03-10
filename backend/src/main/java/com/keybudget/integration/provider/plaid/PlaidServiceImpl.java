package com.keybudget.integration.provider.plaid;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderAuthException;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.exception.ProviderRateLimitException;
import com.keybudget.integration.provider.plaid.config.PlaidConfig;
import com.keybudget.integration.provider.plaid.dto.PlaidLinkTokenCreateRequest;
import com.keybudget.integration.provider.plaid.dto.PlaidLinkTokenCreateResponse;
import com.keybudget.integration.provider.plaid.dto.PlaidPublicTokenExchangeRequest;
import com.keybudget.integration.provider.plaid.dto.PlaidPublicTokenExchangeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * WebClient-based implementation of {@link PlaidService}.
 *
 * <p>All Plaid API calls are made synchronously (via {@code .block()}) with a 10-second
 * timeout to fit the existing blocking provider model. HTTP 400/401 from Plaid are mapped
 * to {@link ProviderAuthException}; HTTP 429 maps to {@link ProviderRateLimitException};
 * all other errors map to {@link ProviderException}.
 */
@Slf4j
@Service
public class PlaidServiceImpl implements PlaidService {

    private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Plaid-backed providers that are valid targets for link-token creation.
     * Any provider not in this set will be rejected at the service layer.
     */
    private static final Set<ProviderType> PLAID_PROVIDERS = Set.of(
            ProviderType.M1_FINANCE,
            ProviderType.MARCUS
    );

    private final WebClient plaidClient;
    private final PlaidConfig plaidConfig;

    /**
     * Constructs a {@code PlaidServiceImpl} with a provider-specific WebClient.
     *
     * @param webClientBuilder pre-configured builder from {@link com.keybudget.config.WebClientConfig}
     * @param plaidConfig      Plaid credentials and environment configuration
     */
    public PlaidServiceImpl(WebClient.Builder webClientBuilder, PlaidConfig plaidConfig) {
        this.plaidConfig = plaidConfig;
        this.plaidClient = webClientBuilder.clone()
                .baseUrl(plaidConfig.getEffectiveBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Products are selected based on the provider:
     * <ul>
     *   <li>{@code M1_FINANCE} uses {@code investments}</li>
     *   <li>{@code MARCUS} uses {@code auth} and {@code transactions}</li>
     * </ul>
     */
    @Override
    public PlaidLinkTokenResult createLinkToken(Long userId, ProviderType provider) {
        if (!PLAID_PROVIDERS.contains(provider)) {
            throw new IllegalArgumentException(
                    "Provider " + provider + " is not a Plaid-backed provider. "
                    + "Supported: " + PLAID_PROVIDERS);
        }

        List<String> products = resolveProducts(provider);

        PlaidLinkTokenCreateRequest requestBody = new PlaidLinkTokenCreateRequest(
                plaidConfig.getClientId(),
                plaidConfig.getSecret(),
                "KeyBudget",
                new PlaidLinkTokenCreateRequest.PlaidUser(String.valueOf(userId)),
                products,
                List.of("US"),
                "en"
        );

        log.info("Creating Plaid link token for userId={}, provider={}, products={}",
                userId, provider, products);

        PlaidLinkTokenCreateResponse response = plaidClient.post()
                .uri("/link/token/create")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(this::isUnauthorized, httpResponse ->
                        Mono.error(new ProviderAuthException(
                                provider,
                                "Plaid rejected credentials — check PLAID_CLIENT_ID and PLAID_SECRET")))
                .onStatus(this::isRateLimit, httpResponse ->
                        Mono.error(new ProviderRateLimitException(
                                provider, "Plaid API rate limit exceeded on /link/token/create")))
                .onStatus(HttpStatusCode::is4xxClientError, httpResponse ->
                        Mono.error(new ProviderException(
                                provider,
                                "Plaid returned client error on /link/token/create: HTTP "
                                        + httpResponse.statusCode().value())))
                .onStatus(HttpStatusCode::is5xxServerError, httpResponse ->
                        Mono.error(new ProviderException(
                                provider,
                                "Plaid returned server error on /link/token/create: HTTP "
                                        + httpResponse.statusCode().value())))
                .bodyToMono(PlaidLinkTokenCreateResponse.class)
                .timeout(API_TIMEOUT)
                .doOnError(ex -> log.error(
                        "Plaid /link/token/create failed for userId={}, provider={}: {}",
                        userId, provider, ex.getMessage()))
                .onErrorMap(ex -> wrapNonProviderException(provider).apply(ex))
                .block();

        if (response == null || response.linkToken() == null) {
            throw new ProviderException(provider, "Plaid returned an empty link token response");
        }

        log.info("Plaid link token created for userId={}, provider={}, expiration={}",
                userId, provider, response.expiration());

        return new PlaidLinkTokenResult(response.linkToken(), response.expiration());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlaidAccessTokenResult exchangePublicToken(String publicToken, ProviderType provider) {
        PlaidPublicTokenExchangeRequest requestBody = new PlaidPublicTokenExchangeRequest(
                plaidConfig.getClientId(),
                plaidConfig.getSecret(),
                publicToken
        );

        log.info("Exchanging Plaid public token for permanent access token");

        PlaidPublicTokenExchangeResponse response = plaidClient.post()
                .uri("/item/public_token/exchange")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(this::isUnauthorized, httpResponse ->
                        Mono.error(new ProviderAuthException(
                                provider,
                                "Plaid rejected the public token — it may have expired or already been exchanged")))
                .onStatus(this::isRateLimit, httpResponse ->
                        Mono.error(new ProviderRateLimitException(
                                provider,
                                "Plaid API rate limit exceeded on /item/public_token/exchange")))
                .onStatus(HttpStatusCode::is4xxClientError, httpResponse ->
                        Mono.error(new ProviderException(
                                provider,
                                "Plaid returned client error on /item/public_token/exchange: HTTP "
                                        + httpResponse.statusCode().value())))
                .onStatus(HttpStatusCode::is5xxServerError, httpResponse ->
                        Mono.error(new ProviderException(
                                provider,
                                "Plaid returned server error on /item/public_token/exchange: HTTP "
                                        + httpResponse.statusCode().value())))
                .bodyToMono(PlaidPublicTokenExchangeResponse.class)
                .timeout(API_TIMEOUT)
                .doOnError(ex -> log.error(
                        "Plaid /item/public_token/exchange failed: {}", ex.getMessage()))
                .onErrorMap(ex -> wrapNonProviderException(provider).apply(ex))
                .block();

        if (response == null || response.accessToken() == null) {
            throw new ProviderException(provider,
                    "Plaid returned an empty token exchange response");
        }

        log.info("Plaid public token exchanged successfully, itemId={}", response.itemId());

        return new PlaidAccessTokenResult(response.accessToken(), response.itemId());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Maps a provider type to the appropriate Plaid products list.
     *
     * @param provider the Plaid-backed provider
     * @return list of Plaid product names
     */
    private List<String> resolveProducts(ProviderType provider) {
        return switch (provider) {
            case M1_FINANCE -> List.of("investments");
            case MARCUS     -> List.of("auth", "transactions");
            default -> throw new IllegalArgumentException(
                    "No product mapping for provider: " + provider);
        };
    }

    /**
     * Returns a function that wraps non-{@link ProviderException} throwables into a
     * {@link ProviderException}, leaving existing {@code ProviderException} subclasses
     * unchanged so {@code onErrorMap} does not re-wrap already-mapped errors.
     *
     * @param provider the provider context for the wrapped exception
     * @return a mapping function suitable for {@code Mono.onErrorMap}
     */
    private java.util.function.Function<Throwable, Throwable> wrapNonProviderException(
            ProviderType provider) {
        return ex -> {
            if (ex instanceof ProviderException) {
                return ex;
            }
            if (ex instanceof WebClientResponseException wcEx) {
                return new ProviderException(provider,
                        "Plaid API error: " + wcEx.getMessage(), wcEx);
            }
            return new ProviderException(provider,
                    "Failed to reach Plaid API: " + ex.getMessage(), ex);
        };
    }

    private boolean isUnauthorized(HttpStatusCode status) {
        return status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN;
    }

    private boolean isRateLimit(HttpStatusCode status) {
        return status == HttpStatus.TOO_MANY_REQUESTS;
    }
}
