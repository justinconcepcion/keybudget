package com.keybudget.integration.provider.marcus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.AccountType;
import com.keybudget.integration.IntegrationProvider.DiscoveredAccount;
import com.keybudget.integration.IntegrationProvider.ProviderBalance;
import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarcusProviderTest {

    private MarcusProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MarcusProvider(new ObjectMapper());
    }

    @Test
    void getProviderType_returnsMarcus() {
        assertThat(provider.getProviderType()).isEqualTo(ProviderType.MARCUS);
    }

    @Nested
    class Connect {

        @Test
        void connect_givenValidBalance_returnsAccount() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "50000.00", "accountName", "My Marcus"));

            assertThat(accounts).hasSize(1);
            DiscoveredAccount account = accounts.get(0);
            assertThat(account.externalId()).isEqualTo("marcus-manual");
            assertThat(account.displayName()).isEqualTo("My Marcus");
            assertThat(account.accountType()).isEqualTo(AccountType.SAVINGS);
            assertThat(account.currency()).isEqualTo("USD");
            assertThat(account.balance()).isEqualByComparingTo("50000.00");
            assertThat(account.balanceUsd()).isEqualByComparingTo("50000.00");
        }

        @Test
        void connect_givenBlankAccountName_usesDefault() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "100.00", "accountName", "  "));

            assertThat(accounts.get(0).displayName()).isEqualTo("Marcus High-Yield Savings");
        }

        @Test
        void connect_givenMissingAccountName_usesDefault() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "100.00"));

            assertThat(accounts.get(0).displayName()).isEqualTo("Marcus High-Yield Savings");
        }

        @Test
        void connect_givenZeroBalance_returnsAccount() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "0"));

            assertThat(accounts.get(0).balance()).isEqualByComparingTo("0.00");
        }

        @Test
        void connect_givenBalanceWithExtraDecimals_roundsToTwoPlaces() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "100.999"));

            assertThat(accounts.get(0).balance()).isEqualByComparingTo("101.00");
        }

        @Test
        void connect_givenNullBalance_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("accountName", "Test")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Balance is required");
        }

        @Test
        void connect_givenBlankBalance_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("balance", "  ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Balance is required");
        }

        @Test
        void connect_givenNegativeBalance_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("balance", "-50.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        void connect_givenNonNumericBalance_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.connect(Map.of("balance", "abc")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid balance format");
        }
    }

    @Nested
    class SyncBalances {

        @Test
        void syncBalances_givenValidCredential_returnsBalance() {
            List<ProviderBalance> balances =
                    provider.syncBalances("{\"balance\":\"50000.00\",\"accountName\":\"Marcus\"}");

            assertThat(balances).hasSize(1);
            ProviderBalance balance = balances.get(0);
            assertThat(balance.externalId()).isEqualTo("marcus-manual");
            assertThat(balance.balance()).isEqualByComparingTo("50000.00");
            assertThat(balance.balanceUsd()).isEqualByComparingTo("50000.00");
            assertThat(balance.asOf()).isNotNull();
        }

        @Test
        void syncBalances_givenMalformedJson_throwsProviderException() {
            assertThatThrownBy(() -> provider.syncBalances("not-json"))
                    .isInstanceOf(ProviderException.class)
                    .hasMessageContaining("Failed to parse stored credential JSON");
        }

        @Test
        void syncBalances_givenMissingBalanceField_throwsIllegalArgument() {
            assertThatThrownBy(() -> provider.syncBalances("{\"accountName\":\"Marcus\"}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Balance is required");
        }
    }

    @Nested
    class ValidateCredential {

        @Test
        void validateCredential_givenValidJson_returnsTrue() {
            assertThat(provider.validateCredential("{\"balance\":\"50000.00\"}")).isTrue();
        }

        @Test
        void validateCredential_givenMalformedJson_returnsFalse() {
            assertThat(provider.validateCredential("INVALID")).isFalse();
        }

        @Test
        void validateCredential_givenMissingBalance_returnsFalse() {
            assertThat(provider.validateCredential("{\"accountName\":\"Marcus\"}")).isFalse();
        }

        @Test
        void validateCredential_givenNegativeBalance_returnsFalse() {
            assertThat(provider.validateCredential("{\"balance\":\"-10\"}")).isFalse();
        }
    }
}
