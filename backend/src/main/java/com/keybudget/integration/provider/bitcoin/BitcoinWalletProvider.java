package com.keybudget.integration.provider.bitcoin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.AccountType;
import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.exception.ProviderRateLimitException;
import com.keybudget.integration.provider.bitcoin.config.BitcoinConfig;
import com.keybudget.integration.provider.bitcoin.config.CoinGeckoConfig;
import com.keybudget.integration.provider.bitcoin.dto.BlockstreamAddressResponse;
import com.keybudget.integration.provider.bitcoin.dto.CoinGeckoPriceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Bitcoin watch-only wallet provider.
 *
 * <p>Uses the public Blockstream Esplora API (no authentication required) to fetch
 * UTXO balance for a given Bitcoin address. BTC/USD conversion uses the CoinGecko
 * public price API (also free, no authentication required).
 *
 * <p>Supported address formats:
 * <ul>
 *   <li>P2PKH — legacy addresses beginning with {@code 1}</li>
 *   <li>P2SH  — script-hash addresses beginning with {@code 3}</li>
 *   <li>Bech32 / SegWit — native SegWit addresses beginning with {@code bc1}</li>
 * </ul>
 *
 * <p>Both external HTTP calls are made synchronously (via {@code .block()}) with a
 * 5-second timeout. CoinGecko failures are treated as non-fatal — the balance in BTC
 * remains authoritative; {@code balanceUsd} will be {@code null} if the price fetch fails.
 */
@Slf4j
@Service
public class BitcoinWalletProvider implements IntegrationProvider {

    private static final long SATOSHIS_PER_BTC = 100_000_000L;
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Accepts P2PKH (1...), P2SH (3...), and Bech32/SegWit (bc1...) address formats.
     * This is a structural sanity check — Blockstream is the authoritative validator.
     */
    private static final Pattern BITCOIN_ADDRESS_PATTERN =
            Pattern.compile("^(1[a-km-zA-HJ-NP-Z1-9]{25,34}|3[a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[a-z0-9]{6,87})$");

    private static final String CREDENTIAL_KEY_ADDRESS = "address";

    private final WebClient blockstreamClient;
    private final WebClient coinGeckoClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@code BitcoinWalletProvider} with provider-specific WebClient instances.
     *
     * @param webClientBuilder pre-configured builder from {@link com.keybudget.config.WebClientConfig}
     * @param bitcoinConfig    Blockstream Esplora base URL configuration
     * @param coinGeckoConfig  CoinGecko price API URL configuration
     * @param objectMapper     shared Jackson mapper for credential JSON parsing
     */
    public BitcoinWalletProvider(
            WebClient.Builder webClientBuilder,
            BitcoinConfig bitcoinConfig,
            CoinGeckoConfig coinGeckoConfig,
            ObjectMapper objectMapper) {
        this.blockstreamClient = webClientBuilder.clone()
                .baseUrl(bitcoinConfig.getBlockstreamUrl())
                .build();
        this.coinGeckoClient = webClientBuilder.clone()
                .baseUrl(coinGeckoConfig.getPriceUrl())
                .build();
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public ProviderType getProviderType() {
        return ProviderType.BITCOIN_WALLET;
    }

    /**
     * Validates a Bitcoin address and fetches its current on-chain balance from Blockstream.
     * A BTC/USD rate is fetched from CoinGecko to populate {@code balanceUsd}.
     *
     * @param credentials must contain key {@code "address"} with a valid Bitcoin address
     * @return a single-element list containing the discovered wallet account
     * @throws IllegalArgumentException if the {@code "address"} key is missing or the
     *                                  address format is invalid
     * @throws ProviderException        if Blockstream returns an error or is unreachable
     */
    @Override
    public List<DiscoveredAccount> connect(Map<String, String> credentials) {
        String address = extractAndValidateAddress(credentials);

        BlockstreamAddressResponse addressInfo = fetchAddressInfo(address);
        BigDecimal balanceBtc = toBalanceBtc(addressInfo);
        BigDecimal btcPriceUsd = fetchBtcPriceUsd();
        BigDecimal balanceUsd = computeBalanceUsd(balanceBtc, btcPriceUsd);

        String displayName = buildDisplayName(address);

        log.info("Bitcoin wallet connected: address={}, balanceBtc={}, balanceUsd={}",
                maskAddress(address), balanceBtc, balanceUsd);

        return List.of(new DiscoveredAccount(
                address,
                displayName,
                AccountType.CRYPTO_WALLET,
                "BTC",
                balanceBtc,
                balanceUsd
        ));
    }

    /**
     * Re-fetches the current on-chain balance for the stored Bitcoin address.
     *
     * @param decryptedCredentialData JSON string containing {@code {"address":"..."}}
     * @return a single-element list with the updated balance
     * @throws ProviderException if the address cannot be parsed or Blockstream is unreachable
     */
    @Override
    public List<ProviderBalance> syncBalances(String decryptedCredentialData) {
        String address = parseAddressFromCredentialJson(decryptedCredentialData);

        BlockstreamAddressResponse addressInfo = fetchAddressInfo(address);
        BigDecimal balanceBtc = toBalanceBtc(addressInfo);
        BigDecimal btcPriceUsd = fetchBtcPriceUsd();
        BigDecimal balanceUsd = computeBalanceUsd(balanceBtc, btcPriceUsd);

        log.debug("Bitcoin wallet synced: address={}, balanceBtc={}, balanceUsd={}",
                maskAddress(address), balanceBtc, balanceUsd);

        return List.of(new ProviderBalance(
                address,
                balanceBtc,
                balanceUsd,
                Instant.now()
        ));
    }

    /**
     * Validates the stored credential by making a lightweight address lookup on Blockstream.
     * Returns {@code true} if Blockstream returns a 200 response, {@code false} otherwise.
     *
     * @param decryptedCredentialData JSON string containing {@code {"address":"..."}}
     * @return {@code true} if the address is reachable and the credential is valid
     */
    @Override
    public boolean validateCredential(String decryptedCredentialData) {
        try {
            String address = parseAddressFromCredentialJson(decryptedCredentialData);
            fetchAddressInfo(address);
            return true;
        } catch (Exception ex) {
            log.warn("Bitcoin credential validation failed: {}", ex.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — address validation
    // -------------------------------------------------------------------------

    /**
     * Extracts and validates the Bitcoin address from the raw credential map supplied
     * at connect time.
     *
     * @param credentials raw credential map from the connect request
     * @return the validated Bitcoin address
     * @throws IllegalArgumentException if {@code "address"} is absent or invalid
     */
    private String extractAndValidateAddress(Map<String, String> credentials) {
        String address = credentials.get(CREDENTIAL_KEY_ADDRESS);
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(
                    "Bitcoin wallet credentials must include a non-blank \"address\" field");
        }
        validateAddressFormat(address.trim());
        return address.trim();
    }

    /**
     * Validates the Bitcoin address format against the supported address pattern.
     *
     * @param address the address to validate
     * @throws IllegalArgumentException if the format is not recognised
     */
    private void validateAddressFormat(String address) {
        if (!BITCOIN_ADDRESS_PATTERN.matcher(address).matches()) {
            throw new IllegalArgumentException(
                    "Invalid Bitcoin address format. Supported formats: P2PKH (1...), "
                            + "P2SH (3...), Bech32/SegWit (bc1...)");
        }
    }

    /**
     * Parses the Bitcoin address from the stored credential JSON blob.
     *
     * @param decryptedCredentialData JSON string, e.g. {@code {"address":"bc1q..."}}
     * @return the address value
     * @throws ProviderException if parsing fails
     */
    private String parseAddressFromCredentialJson(String decryptedCredentialData) {
        try {
            Map<String, String> credMap = objectMapper.readValue(
                    decryptedCredentialData,
                    new TypeReference<Map<String, String>>() {});
            String address = credMap.get(CREDENTIAL_KEY_ADDRESS);
            if (address == null || address.isBlank()) {
                throw new ProviderException(ProviderType.BITCOIN_WALLET,
                        "Stored credential is missing the \"address\" field");
            }
            return address.trim();
        } catch (JsonProcessingException ex) {
            throw new ProviderException(ProviderType.BITCOIN_WALLET,
                    "Failed to parse stored Bitcoin credential JSON", ex);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — Blockstream API
    // -------------------------------------------------------------------------

    /**
     * Calls the Blockstream Esplora {@code /address/{address}} endpoint and returns
     * the parsed response.
     *
     * @param address Bitcoin address to query
     * @return parsed Blockstream address response
     * @throws ProviderException         on HTTP error or network timeout
     * @throws ProviderRateLimitException if Blockstream returns HTTP 429
     */
    private BlockstreamAddressResponse fetchAddressInfo(String address) {
        try {
            return blockstreamClient.get()
                    .uri("/address/{address}", address)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        if (response.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            return Mono.error(new ProviderRateLimitException(
                                    ProviderType.BITCOIN_WALLET,
                                    "Blockstream API rate limit exceeded"));
                        }
                        // 400 / 404 most likely means the address is unrecognised by Blockstream
                        return Mono.error(new IllegalArgumentException(
                                "Blockstream rejected address — HTTP " + response.statusCode().value()
                                        + ". Verify the address is a valid mainnet Bitcoin address."));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response ->
                            Mono.error(new ProviderException(
                                    ProviderType.BITCOIN_WALLET,
                                    "Blockstream API returned server error: HTTP "
                                            + response.statusCode().value())))
                    .bodyToMono(BlockstreamAddressResponse.class)
                    .timeout(API_TIMEOUT)
                    .block();
        } catch (ProviderException | IllegalArgumentException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new ProviderException(ProviderType.BITCOIN_WALLET,
                    "Blockstream API error: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ProviderException(ProviderType.BITCOIN_WALLET,
                    "Failed to reach Blockstream API: " + ex.getMessage(), ex);
        }
    }

    /**
     * Derives the confirmed on-chain BTC balance from the Blockstream response.
     * Mempool (unconfirmed) transactions are intentionally excluded.
     *
     * @param response Blockstream address response
     * @return confirmed balance in BTC, with 8 decimal places precision
     */
    private BigDecimal toBalanceBtc(BlockstreamAddressResponse response) {
        long satoshis = response.chainStats().fundedTxoSum()
                - response.chainStats().spentTxoSum();
        return BigDecimal.valueOf(satoshis)
                .divide(BigDecimal.valueOf(SATOSHIS_PER_BTC), 8, RoundingMode.UNNECESSARY);
    }

    // -------------------------------------------------------------------------
    // Private helpers — CoinGecko API
    // -------------------------------------------------------------------------

    /**
     * Fetches the current BTC/USD price from CoinGecko.
     * This call is treated as best-effort — a {@code null} return signals that the
     * USD conversion is unavailable but the BTC balance itself is still valid.
     *
     * @return BTC price in USD, or {@code null} if CoinGecko is unreachable or rate-limited
     */
    private BigDecimal fetchBtcPriceUsd() {
        try {
            CoinGeckoPriceResponse response = coinGeckoClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("ids", "bitcoin")
                            .queryParam("vs_currencies", "usd")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, httpResponse -> {
                        if (httpResponse.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                            return Mono.error(new RuntimeException("CoinGecko rate limit exceeded"));
                        }
                        return Mono.error(new RuntimeException(
                                "CoinGecko returned HTTP " + httpResponse.statusCode().value()));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, httpResponse ->
                            Mono.error(new RuntimeException(
                                    "CoinGecko server error: HTTP " + httpResponse.statusCode().value())))
                    .bodyToMono(CoinGeckoPriceResponse.class)
                    .timeout(API_TIMEOUT)
                    .block();

            if (response == null || response.bitcoin() == null) {
                log.warn("CoinGecko returned an empty price response; balanceUsd will be null");
                return null;
            }
            return BigDecimal.valueOf(response.bitcoin().usd())
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            log.warn("Failed to fetch BTC/USD price from CoinGecko; balanceUsd will be null. Reason: {}",
                    ex.getMessage());
            return null;
        }
    }

    /**
     * Multiplies the BTC balance by the USD price to derive the USD equivalent.
     *
     * @param balanceBtc  BTC balance
     * @param btcPriceUsd current BTC/USD price; may be {@code null}
     * @return USD balance rounded to 2 decimal places, or {@code null} if price unavailable
     */
    private BigDecimal computeBalanceUsd(BigDecimal balanceBtc, BigDecimal btcPriceUsd) {
        if (btcPriceUsd == null) {
            return null;
        }
        return balanceBtc.multiply(btcPriceUsd, MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // -------------------------------------------------------------------------
    // Private helpers — display
    // -------------------------------------------------------------------------

    /**
     * Builds a human-readable wallet display name using a shortened version of the address.
     * Format: {@code "Bitcoin Wallet (...XXXXX)"} where XXXXX is the last 6 characters.
     *
     * @param address full Bitcoin address
     * @return display name string
     */
    private String buildDisplayName(String address) {
        String tail = address.substring(Math.max(0, address.length() - 6));
        return "Bitcoin Wallet (..." + tail + ")";
    }

    /**
     * Returns a masked version of the address for safe logging (first 6 + last 4 chars).
     *
     * @param address full Bitcoin address
     * @return masked address string
     */
    private String maskAddress(String address) {
        if (address.length() <= 10) {
            return "***";
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }
}
