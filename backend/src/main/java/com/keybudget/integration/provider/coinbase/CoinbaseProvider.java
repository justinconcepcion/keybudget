package com.keybudget.integration.provider.coinbase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.AccountType;
import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderAuthException;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.exception.ProviderRateLimitException;
import com.keybudget.integration.provider.coinbase.config.CoinbaseConfig;
import com.keybudget.integration.provider.coinbase.dto.CoinbaseAccount;
import com.keybudget.integration.provider.coinbase.dto.CoinbaseAccountsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Coinbase integration provider using Coinbase API v2 with HMAC-SHA256 authentication.
 *
 * <p>Authentication follows Coinbase's API key + secret scheme: every request carries a
 * {@code CB-ACCESS-SIGN} header computed as the hex-encoded HMAC-SHA256 of
 * {@code timestamp + method + requestPath + body} signed with the API secret.
 *
 * <p>Pagination is handled transparently — all pages of {@code /v2/accounts} are fetched
 * and merged before returning results to the caller.
 *
 * <p>Error mapping:
 * <ul>
 *   <li>HTTP 401 from Coinbase → {@link ProviderAuthException}</li>
 *   <li>HTTP 429 from Coinbase → {@link ProviderRateLimitException}</li>
 *   <li>Any other error → {@link ProviderException}</li>
 * </ul>
 *
 * <p>Cost note: Coinbase API v2 read-only access (accounts, balances) is free.
 */
@Slf4j
@Service
public class CoinbaseProvider implements IntegrationProvider {

    private static final String CREDENTIAL_KEY_API_KEY = "apiKey";
    private static final String CREDENTIAL_KEY_API_SECRET = "apiSecret";
    private static final String ACCOUNTS_PATH = "/v2/accounts";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration API_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient coinbaseClient;
    private final ObjectMapper objectMapper;
    private final CoinbaseConfig coinbaseConfig;

    /**
     * Constructs a {@code CoinbaseProvider} with a provider-scoped WebClient.
     *
     * @param webClientBuilder pre-configured builder from {@link com.keybudget.config.WebClientConfig}
     * @param objectMapper     shared Jackson mapper for credential JSON parsing
     * @param coinbaseConfig   externalized Coinbase API configuration
     */
    public CoinbaseProvider(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            CoinbaseConfig coinbaseConfig) {
        this.coinbaseClient = webClientBuilder.clone()
                .baseUrl(coinbaseConfig.getApiUrl())
                .build();
        this.objectMapper = objectMapper;
        this.coinbaseConfig = coinbaseConfig;
    }

    /** {@inheritDoc} */
    @Override
    public ProviderType getProviderType() {
        return ProviderType.COINBASE;
    }

    /**
     * Validates the supplied API key and secret by listing all Coinbase accounts, then
     * maps each discovered account to a {@link DiscoveredAccount}.
     *
     * <p>Credentials map must contain:
     * <ul>
     *   <li>{@code "apiKey"} — Coinbase API key</li>
     *   <li>{@code "apiSecret"} — Coinbase API secret used for HMAC signing</li>
     * </ul>
     *
     * @param credentials raw key/value credential map from the connect request
     * @return list of all discovered Coinbase accounts with native and USD balances
     * @throws IllegalArgumentException if {@code "apiKey"} or {@code "apiSecret"} are absent
     * @throws ProviderAuthException    if Coinbase returns HTTP 401
     * @throws ProviderRateLimitException if Coinbase returns HTTP 429
     * @throws ProviderException        on any other provider error
     */
    @Override
    public List<DiscoveredAccount> connect(Map<String, String> credentials) {
        String apiKey = extractRequiredCredential(credentials, CREDENTIAL_KEY_API_KEY);
        String apiSecret = extractRequiredCredential(credentials, CREDENTIAL_KEY_API_SECRET);

        List<CoinbaseAccount> accounts = fetchAllAccounts(apiKey, apiSecret);
        List<DiscoveredAccount> discovered = accounts.stream()
                .map(this::toDiscoveredAccount)
                .toList();

        log.info("Coinbase connect: discovered {} accounts for key={}",
                discovered.size(), maskKey(apiKey));
        return discovered;
    }

    /**
     * Re-fetches balances for all accounts reachable with the stored credentials.
     * The credential JSON must contain {@code apiKey} and {@code apiSecret} fields.
     *
     * @param decryptedCredentialData JSON string, e.g. {@code {"apiKey":"...","apiSecret":"..."}}
     * @return list of {@link ProviderBalance} records, one per Coinbase account
     * @throws ProviderException        if the credential JSON cannot be parsed
     * @throws ProviderAuthException    if Coinbase rejects the stored credentials
     * @throws ProviderRateLimitException if Coinbase returns HTTP 429
     */
    @Override
    public List<ProviderBalance> syncBalances(String decryptedCredentialData) {
        Map<String, String> credMap = parseCredentialJson(decryptedCredentialData);
        String apiKey = extractRequiredCredential(credMap, CREDENTIAL_KEY_API_KEY);
        String apiSecret = extractRequiredCredential(credMap, CREDENTIAL_KEY_API_SECRET);

        Instant asOf = Instant.now();
        List<CoinbaseAccount> accounts = fetchAllAccounts(apiKey, apiSecret);
        List<ProviderBalance> balances = accounts.stream()
                .map(account -> toProviderBalance(account, asOf))
                .toList();

        log.debug("Coinbase sync: fetched {} balances for key={}", balances.size(), maskKey(apiKey));
        return balances;
    }

    /**
     * Performs a lightweight credential check by attempting to list Coinbase accounts.
     * Returns {@code true} on a successful HTTP 200 response, {@code false} on any failure.
     *
     * @param decryptedCredentialData JSON string containing {@code apiKey} and {@code apiSecret}
     * @return {@code true} if the credentials are accepted by Coinbase
     */
    @Override
    public boolean validateCredential(String decryptedCredentialData) {
        try {
            Map<String, String> credMap = parseCredentialJson(decryptedCredentialData);
            String apiKey = extractRequiredCredential(credMap, CREDENTIAL_KEY_API_KEY);
            String apiSecret = extractRequiredCredential(credMap, CREDENTIAL_KEY_API_SECRET);
            fetchAllAccounts(apiKey, apiSecret);
            return true;
        } catch (Exception ex) {
            log.warn("Coinbase credential validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — API fetch with pagination
    // -------------------------------------------------------------------------

    /**
     * Fetches all pages of {@code /v2/accounts}, following {@code pagination.next_uri}
     * until it is null. All accounts across all pages are merged into a single list.
     *
     * @param apiKey    Coinbase API key
     * @param apiSecret Coinbase API secret
     * @return complete list of Coinbase accounts across all pages
     * @throws ProviderAuthException      if any page request returns HTTP 401
     * @throws ProviderRateLimitException if any page request returns HTTP 429
     * @throws ProviderException          on HTTP 5xx or network errors
     */
    private List<CoinbaseAccount> fetchAllAccounts(String apiKey, String apiSecret) {
        List<CoinbaseAccount> allAccounts = new ArrayList<>();
        String nextPath = ACCOUNTS_PATH;

        while (nextPath != null) {
            CoinbaseAccountsResponse page = fetchAccountsPage(apiKey, apiSecret, nextPath);
            if (page.data() != null) {
                allAccounts.addAll(page.data());
            }
            nextPath = (page.pagination() != null) ? page.pagination().nextUri() : null;
        }

        return allAccounts;
    }

    /**
     * Fetches a single page from the given Coinbase accounts path, applying all
     * required HMAC-SHA256 authentication headers.
     *
     * @param apiKey    Coinbase API key
     * @param apiSecret Coinbase API secret for HMAC signing
     * @param path      request path (e.g., {@code /v2/accounts} or a paginated path)
     * @return parsed {@link CoinbaseAccountsResponse} for this page
     * @throws ProviderAuthException      on HTTP 401
     * @throws ProviderRateLimitException on HTTP 429
     * @throws ProviderException          on other HTTP errors or connection failures
     */
    private CoinbaseAccountsResponse fetchAccountsPage(String apiKey, String apiSecret, String path) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = signRequest(apiSecret, timestamp, "GET", path);

        try {
            return coinbaseClient.get()
                    .uri(path)
                    .header("CB-ACCESS-KEY", apiKey)
                    .header("CB-ACCESS-SIGN", signature)
                    .header("CB-ACCESS-TIMESTAMP", timestamp)
                    .header("CB-VERSION", coinbaseConfig.getApiVersion())
                    .retrieve()
                    .onStatus(status -> status == HttpStatus.UNAUTHORIZED, response ->
                            Mono.error(new ProviderAuthException(
                                    ProviderType.COINBASE,
                                    "Invalid API key or secret")))
                    .onStatus(status -> status == HttpStatus.TOO_MANY_REQUESTS, response ->
                            Mono.error(new ProviderRateLimitException(
                                    ProviderType.COINBASE,
                                    "Coinbase API rate limit exceeded (max ~10,000 requests/hour)")))
                    .onStatus(HttpStatusCode::is4xxClientError, response ->
                            Mono.error(new ProviderException(
                                    ProviderType.COINBASE,
                                    "Coinbase API client error: HTTP " + response.statusCode().value())))
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new ProviderException(
                                    ProviderType.COINBASE,
                                    "Coinbase API server error: HTTP " + response.statusCode().value())))
                    .bodyToMono(CoinbaseAccountsResponse.class)
                    .timeout(API_TIMEOUT)
                    .block();
        } catch (ProviderException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new ProviderException(ProviderType.COINBASE,
                    "Coinbase API error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ProviderException(ProviderType.COINBASE,
                    "Failed to reach Coinbase API: " + ex.getMessage(), ex);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — HMAC-SHA256 signing
    // -------------------------------------------------------------------------

    /**
     * Generates the HMAC-SHA256 signature required by the Coinbase API v2 authentication scheme.
     *
     * <p>The message to sign is the concatenation of:
     * {@code timestamp + method + requestPath + body}.
     * For GET requests the body is an empty string.
     * The resulting HMAC bytes are hex-encoded (lowercase), as required by Coinbase.
     *
     * @param apiSecret Coinbase API secret (UTF-8 encoded as the HMAC key)
     * @param timestamp Unix epoch seconds as a string, matching {@code CB-ACCESS-TIMESTAMP}
     * @param method    HTTP method in uppercase (e.g., {@code "GET"})
     * @param path      full request path including query string (e.g., {@code "/v2/accounts"})
     * @return hex-encoded HMAC-SHA256 signature string
     * @throws ProviderException if the JVM does not support HMAC-SHA256 (should never happen)
     */
    private String signRequest(String apiSecret, String timestamp, String method, String path) {
        String message = timestamp + method + path;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    apiSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(
                    message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException ex) {
            // HmacSHA256 is mandated by the JVM spec — this cannot happen in practice
            throw new ProviderException(ProviderType.COINBASE,
                    "HMAC-SHA256 algorithm not available on this JVM", ex);
        } catch (InvalidKeyException ex) {
            throw new ProviderException(ProviderType.COINBASE,
                    "Invalid Coinbase API secret for HMAC signing", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a raw {@link CoinbaseAccount} from the API response to a {@link DiscoveredAccount}.
     * Amounts are parsed from their string representation to {@link BigDecimal}.
     * Zero-balance accounts are included — callers may filter them if needed.
     *
     * @param account Coinbase API account object
     * @return a {@link DiscoveredAccount} representing the account at discovery time
     */
    private DiscoveredAccount toDiscoveredAccount(CoinbaseAccount account) {
        BigDecimal balance = parseAmount(account.balance() != null ? account.balance().amount() : null);
        BigDecimal balanceUsd = parseAmount(
                account.nativeBalance() != null ? account.nativeBalance().amount() : null);
        String currency = account.balance() != null ? account.balance().currency() : "UNKNOWN";

        return new DiscoveredAccount(
                account.id(),
                account.name(),
                AccountType.CRYPTO_WALLET,
                currency,
                balance,
                balanceUsd
        );
    }

    /**
     * Maps a raw {@link CoinbaseAccount} to a {@link ProviderBalance} at sync time.
     *
     * @param account Coinbase API account object
     * @param asOf    timestamp at which the balance was fetched
     * @return a {@link ProviderBalance} with native and USD balances
     */
    private ProviderBalance toProviderBalance(CoinbaseAccount account, Instant asOf) {
        BigDecimal balance = parseAmount(account.balance() != null ? account.balance().amount() : null);
        BigDecimal balanceUsd = parseAmount(
                account.nativeBalance() != null ? account.nativeBalance().amount() : null);

        return new ProviderBalance(
                account.id(),
                balance,
                balanceUsd,
                asOf
        );
    }

    /**
     * Parses a decimal amount string from the Coinbase API into a {@link BigDecimal},
     * scaled to 8 decimal places. Returns {@link BigDecimal#ZERO} if the string is null
     * or blank to avoid null propagation in balance arithmetic.
     *
     * @param amount string representation of the amount (e.g., {@code "0.05432100"})
     * @return parsed {@link BigDecimal}, or {@code BigDecimal.ZERO} if input is null/blank
     */
    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(amount).setScale(8, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            log.warn("Coinbase returned an unparseable amount '{}'; defaulting to 0", amount);
            return BigDecimal.ZERO;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — credential parsing
    // -------------------------------------------------------------------------

    /**
     * Parses the decrypted credential JSON blob into a key/value map.
     *
     * @param decryptedCredentialData JSON string, e.g. {@code {"apiKey":"...","apiSecret":"..."}}
     * @return parsed credential map
     * @throws ProviderException if the JSON cannot be parsed
     */
    private Map<String, String> parseCredentialJson(String decryptedCredentialData) {
        try {
            return objectMapper.readValue(
                    decryptedCredentialData,
                    new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException ex) {
            throw new ProviderException(ProviderType.COINBASE,
                    "Failed to parse stored Coinbase credential JSON", ex);
        }
    }

    /**
     * Extracts a required credential value from a credential map.
     *
     * @param credentials credential map
     * @param key         the required key
     * @return the non-blank value for the key
     * @throws IllegalArgumentException if the key is absent or the value is blank
     */
    private String extractRequiredCredential(Map<String, String> credentials, String key) {
        String value = credentials.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Coinbase credentials must include a non-blank \"" + key + "\" field");
        }
        return value.trim();
    }

    // -------------------------------------------------------------------------
    // Private helpers — safe logging
    // -------------------------------------------------------------------------

    /**
     * Returns a masked version of the API key suitable for log output.
     * Shows only the first 4 and last 4 characters to aid debugging without
     * exposing the full key.
     *
     * @param apiKey full Coinbase API key
     * @return masked key string, e.g. {@code "abcd...wxyz"}
     */
    private String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
