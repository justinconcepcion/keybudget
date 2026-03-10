package com.keybudget.integration.provider.m1finance;

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

class M1FinanceProviderTest {

    private M1FinanceProvider provider;

    @BeforeEach
    void setUp() {
        provider = new M1FinanceProvider(new ObjectMapper());
    }

    @Test
    void getProviderType_returnsM1Finance() {
        assertThat(provider.getProviderType()).isEqualTo(ProviderType.M1_FINANCE);
    }

    @Nested
    class Connect {

        @Test
        void connect_givenValidBalance_returnsAccount() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "12345.67", "accountName", "My M1"));

            assertThat(accounts).hasSize(1);
            DiscoveredAccount account = accounts.get(0);
            assertThat(account.externalId()).isEqualTo("m1-manual");
            assertThat(account.displayName()).isEqualTo("My M1");
            assertThat(account.accountType()).isEqualTo(AccountType.BROKERAGE);
            assertThat(account.currency()).isEqualTo("USD");
            assertThat(account.balance()).isEqualByComparingTo("12345.67");
            assertThat(account.balanceUsd()).isEqualByComparingTo("12345.67");
        }

        @Test
        void connect_givenBlankAccountName_usesDefault() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "100.00", "accountName", "  "));

            assertThat(accounts.get(0).displayName()).isEqualTo("M1 Finance Brokerage");
        }

        @Test
        void connect_givenMissingAccountName_usesDefault() {
            List<DiscoveredAccount> accounts =
                    provider.connect(Map.of("balance", "100.00"));

            assertThat(accounts.get(0).displayName()).isEqualTo("M1 Finance Brokerage");
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
                    provider.syncBalances("{\"balance\":\"5000.50\",\"accountName\":\"M1\"}");

            assertThat(balances).hasSize(1);
            ProviderBalance balance = balances.get(0);
            assertThat(balance.externalId()).isEqualTo("m1-manual");
            assertThat(balance.balance()).isEqualByComparingTo("5000.50");
            assertThat(balance.balanceUsd()).isEqualByComparingTo("5000.50");
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
            assertThatThrownBy(() -> provider.syncBalances("{\"accountName\":\"M1\"}"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Balance is required");
        }
    }

    @Nested
    class ValidateCredential {

        @Test
        void validateCredential_givenValidJson_returnsTrue() {
            assertThat(provider.validateCredential("{\"balance\":\"100.00\"}")).isTrue();
        }

        @Test
        void validateCredential_givenMalformedJson_returnsFalse() {
            assertThat(provider.validateCredential("INVALID")).isFalse();
        }

        @Test
        void validateCredential_givenMissingBalance_returnsFalse() {
            assertThat(provider.validateCredential("{\"accountName\":\"M1\"}")).isFalse();
        }

        @Test
        void validateCredential_givenNegativeBalance_returnsFalse() {
            assertThat(provider.validateCredential("{\"balance\":\"-10\"}")).isFalse();
        }
    }
}
