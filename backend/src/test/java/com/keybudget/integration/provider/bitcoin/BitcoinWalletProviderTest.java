package com.keybudget.integration.provider.bitcoin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.IntegrationProvider.DiscoveredAccount;
import com.keybudget.integration.IntegrationProvider.ProviderBalance;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.provider.bitcoin.config.BitcoinConfig;
import com.keybudget.integration.provider.bitcoin.config.CoinGeckoConfig;
import com.keybudget.integration.provider.bitcoin.dto.BlockstreamAddressResponse;
import com.keybudget.integration.provider.bitcoin.dto.BlockstreamAddressResponse.ChainStats;
import com.keybudget.integration.provider.bitcoin.dto.BlockstreamAddressResponse.MempoolStats;
import com.keybudget.integration.provider.bitcoin.dto.CoinGeckoPriceResponse;
import com.keybudget.integration.provider.bitcoin.dto.CoinGeckoPriceResponse.BitcoinPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BitcoinWalletProvider}.
 *
 * WebClient is mocked via its builder chain so no real HTTP calls are made.
 * The builder is cloned twice (once for Blockstream, once for CoinGecko), so
 * we set up the clone() stub to return itself, allowing the same mock chain to
 * be used for both clients.
 */
@ExtendWith(MockitoExtension.class)
class BitcoinWalletProviderTest {

    // --- Valid test addresses -------------------------------------------------
    private static final String BECH32_ADDRESS = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";
    private static final String LEGACY_ADDRESS  = "1A1zP1eP5QGefi2DMPTfTL5SLmv7Divf";
    private static final String P2SH_ADDRESS    = "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy";
    private static final String INVALID_ADDRESS = "notabitcoinaddress";

    private static final String CRED_JSON = "{\"address\":\"" + BECH32_ADDRESS + "\"}";

    // --- WebClient mock hierarchy --------------------------------------------
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private WebClient blockstreamWebClient;
    @Mock private WebClient coinGeckoWebClient;

    // Blockstream chain
    @Mock private WebClient.RequestHeadersUriSpec<?> bsUriSpec;
    @Mock private WebClient.RequestHeadersSpec<?> bsHeadersSpec;
    @Mock private WebClient.ResponseSpec bsResponseSpec;

    // CoinGecko chain
    @Mock private WebClient.RequestHeadersUriSpec<?> cgUriSpec;
    @Mock private WebClient.RequestHeadersSpec<?> cgHeadersSpec;
    @Mock private WebClient.ResponseSpec cgResponseSpec;

    private BitcoinWalletProvider provider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // builder.clone() is called twice — once per client.
        // First clone builds the Blockstream client, second builds CoinGecko.
        when(webClientBuilder.clone()).thenReturn(webClientBuilder);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build())
                .thenReturn(blockstreamWebClient)
                .thenReturn(coinGeckoWebClient);

        BitcoinConfig bitcoinConfig = new BitcoinConfig();
        bitcoinConfig.setBlockstreamUrl("https://blockstream.info/api");

        CoinGeckoConfig coinGeckoConfig = new CoinGeckoConfig();
        coinGeckoConfig.setPriceUrl("https://api.coingecko.com/api/v3/simple/price");

        provider = new BitcoinWalletProvider(
                webClientBuilder,
                bitcoinConfig,
                coinGeckoConfig,
                new ObjectMapper()
        );
    }

    // -------------------------------------------------------------------------
    // connect()
    // -------------------------------------------------------------------------

    @Nested
    class Connect {

        @Test
        @SuppressWarnings("unchecked")
        void connect_givenValidBech32Address_returnsAccount() {
            stubBlockstream(BECH32_ADDRESS, buildAddressResponse(150_000_000L, 50_000_000L));
            stubCoinGecko(60_000.0);

            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("address", BECH32_ADDRESS));

            assertThat(accounts).hasSize(1);
            DiscoveredAccount account = accounts.get(0);
            assertThat(account.externalId()).isEqualTo(BECH32_ADDRESS);
            assertThat(account.currency()).isEqualTo("BTC");
            assertThat(account.balance()).isEqualByComparingTo("1.00000000"); // 100_000_000 sat
            assertThat(account.balanceUsd()).isEqualByComparingTo("60000.00");
            assertThat(account.displayName()).contains("Bitcoin Wallet");
        }

        @Test
        @SuppressWarnings("unchecked")
        void connect_givenValidLegacyAddress_returnsAccount() {
            stubBlockstream(LEGACY_ADDRESS, buildAddressResponse(100_000_000L, 0L));
            stubCoinGecko(50_000.0);

            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("address", LEGACY_ADDRESS));

            assertThat(accounts).hasSize(1);
            assertThat(accounts.get(0).balance()).isEqualByComparingTo("1.00000000");
        }

        @Test
        void connect_givenInvalidAddressFormat_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("address", INVALID_ADDRESS)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Bitcoin address format");
        }

        @Test
        void connect_givenMissingAddressKey_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("apiKey", "something")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address");
        }

        @Test
        void connect_givenBlankAddress_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("address", "  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address");
        }

        @Test
        @SuppressWarnings("unchecked")
        void connect_givenBlockstreamError_throwsProviderException() {
            stubBlockstreamError(new WebClientResponseException(
                    500, "Internal Server Error", null, null, null));

            assertThatThrownBy(() -> provider.connect(Map.of("address", BECH32_ADDRESS)))
                    .isInstanceOf(ProviderException.class);
        }

        @Test
        @SuppressWarnings("unchecked")
        void connect_givenCoinGeckoError_returnsAccountWithNullBalanceUsd() {
            stubBlockstream(BECH32_ADDRESS, buildAddressResponse(100_000_000L, 0L));
            stubCoinGeckoError(new RuntimeException("CoinGecko unavailable"));

            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("address", BECH32_ADDRESS));

            assertThat(accounts).hasSize(1);
            assertThat(accounts.get(0).balance()).isEqualByComparingTo("1.00000000");
            assertThat(accounts.get(0).balanceUsd()).isNull();
        }

        @Test
        @SuppressWarnings("unchecked")
        void connect_givenZeroBalance_returnsAccountWithZeroBalance() {
            stubBlockstream(BECH32_ADDRESS, buildAddressResponse(0L, 0L));
            stubCoinGecko(60_000.0);

            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("address", BECH32_ADDRESS));

            assertThat(accounts).hasSize(1);
            assertThat(accounts.get(0).balance()).isEqualByComparingTo("0.00000000");
            assertThat(accounts.get(0).balanceUsd()).isEqualByComparingTo("0.00");
        }
    }

    // -------------------------------------------------------------------------
    // syncBalances()
    // -------------------------------------------------------------------------

    @Nested
    class SyncBalances {

        @Test
        @SuppressWarnings("unchecked")
        void syncBalances_givenValidCredential_returnsBalance() {
            stubBlockstream(BECH32_ADDRESS, buildAddressResponse(250_000_000L, 50_000_000L));
            stubCoinGecko(50_000.0);

            List<ProviderBalance> balances = provider.syncBalances(CRED_JSON);

            assertThat(balances).hasSize(1);
            ProviderBalance balance = balances.get(0);
            assertThat(balance.externalId()).isEqualTo(BECH32_ADDRESS);
            assertThat(balance.balance()).isEqualByComparingTo("2.00000000"); // 200_000_000 sat
            assertThat(balance.balanceUsd()).isEqualByComparingTo("100000.00");
            assertThat(balance.asOf()).isNotNull();
        }

        @Test
        @SuppressWarnings("unchecked")
        void syncBalances_givenBlockstreamTimeout_throwsProviderException() {
            stubBlockstreamError(new ProviderException(
                    com.keybudget.integration.ProviderType.BITCOIN_WALLET,
                    "Failed to reach Blockstream API: timeout"));

            assertThatThrownBy(() -> provider.syncBalances(CRED_JSON))
                    .isInstanceOf(ProviderException.class);
        }

        @Test
        void syncBalances_givenMalformedCredentialJson_throwsProviderException() {
            assertThatThrownBy(() -> provider.syncBalances("not-json"))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("Failed to parse stored Bitcoin credential JSON");
        }

        @Test
        void syncBalances_givenCredentialMissingAddressField_throwsProviderException() {
            assertThatThrownBy(() -> provider.syncBalances("{\"other\":\"value\"}"))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("address");
        }
    }

    // -------------------------------------------------------------------------
    // validateCredential()
    // -------------------------------------------------------------------------

    @Nested
    class ValidateCredential {

        @Test
        @SuppressWarnings("unchecked")
        void validateCredential_givenValidAddress_returnsTrue() {
            stubBlockstream(BECH32_ADDRESS, buildAddressResponse(0L, 0L));

            boolean result = provider.validateCredential(CRED_JSON);

            assertThat(result).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void validateCredential_givenBlockstreamError_returnsFalse() {
            stubBlockstreamError(new RuntimeException("Connection refused"));

            boolean result = provider.validateCredential(CRED_JSON);

            assertThat(result).isFalse();
        }

        @Test
        void validateCredential_givenMalformedJson_returnsFalse() {
            boolean result = provider.validateCredential("INVALID_JSON");

            assertThat(result).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // getProviderType()
    // -------------------------------------------------------------------------

    @Test
    void getProviderType_returnsBitcoinWallet() {
        assertThat(provider.getProviderType())
                .isEqualTo(com.keybudget.integration.ProviderType.BITCOIN_WALLET);
    }

    // -------------------------------------------------------------------------
    // Stub helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubBlockstream(String address, BlockstreamAddressResponse response) {
        when(blockstreamWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) bsUriSpec);
        when(bsUriSpec.uri(anyString(), (Object) any())).thenReturn((WebClient.RequestHeadersSpec) bsHeadersSpec);
        when(bsHeadersSpec.retrieve()).thenReturn(bsResponseSpec);
        when(bsResponseSpec.onStatus(any(), any())).thenReturn(bsResponseSpec);
        when(bsResponseSpec.bodyToMono(BlockstreamAddressResponse.class))
                .thenReturn(Mono.just(response));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubBlockstreamError(Exception error) {
        when(blockstreamWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) bsUriSpec);
        when(bsUriSpec.uri(anyString(), (Object) any())).thenReturn((WebClient.RequestHeadersSpec) bsHeadersSpec);
        when(bsHeadersSpec.retrieve()).thenReturn(bsResponseSpec);
        when(bsResponseSpec.onStatus(any(), any())).thenReturn(bsResponseSpec);
        when(bsResponseSpec.bodyToMono(BlockstreamAddressResponse.class))
                .thenReturn(Mono.error(error));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubCoinGecko(double btcPriceUsd) {
        CoinGeckoPriceResponse priceResponse =
                new CoinGeckoPriceResponse(new BitcoinPrice(btcPriceUsd));
        when(coinGeckoWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) cgUriSpec);
        when(cgUriSpec.uri(any(java.util.function.Function.class)))
                .thenReturn((WebClient.RequestHeadersSpec) cgHeadersSpec);
        when(cgHeadersSpec.retrieve()).thenReturn(cgResponseSpec);
        when(cgResponseSpec.onStatus(any(), any())).thenReturn(cgResponseSpec);
        when(cgResponseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.just(priceResponse));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubCoinGeckoError(Exception error) {
        when(coinGeckoWebClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) cgUriSpec);
        when(cgUriSpec.uri(any(java.util.function.Function.class)))
                .thenReturn((WebClient.RequestHeadersSpec) cgHeadersSpec);
        when(cgHeadersSpec.retrieve()).thenReturn(cgResponseSpec);
        when(cgResponseSpec.onStatus(any(), any())).thenReturn(cgResponseSpec);
        when(cgResponseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.error(error));
    }

    private BlockstreamAddressResponse buildAddressResponse(long funded, long spent) {
        ChainStats chainStats = new ChainStats(1, funded, 0, spent, 1);
        MempoolStats mempoolStats = new MempoolStats(0, 0, 0, 0, 0);
        return new BlockstreamAddressResponse(BECH32_ADDRESS, chainStats, mempoolStats);
    }
}
