package com.keybudget.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.dto.*;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.model.*;
import com.keybudget.integration.repository.*;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.shared.encryption.EncryptionService;
import com.keybudget.user.User;
import com.keybudget.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationOrchestrationServiceImplTest {

    private static final long USER_ID       = 1L;
    private static final long CREDENTIAL_ID = 10L;
    private static final long ACCOUNT_ID    = 100L;

    private static final String EXTERNAL_ID = "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq";

    @Mock private IntegrationProvider bitcoinProvider;
    @Mock private EncryptionService encryptionService;
    @Mock private UserRepository userRepository;
    @Mock private IntegrationCredentialRepository credentialRepository;
    @Mock private FinancialAccountRepository accountRepository;
    @Mock private FinancialAccountBalanceRepository balanceRepository;
    @Mock private BalanceSnapshotRepository snapshotRepository;

    private IntegrationOrchestrationServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        when(bitcoinProvider.getProviderType()).thenReturn(ProviderType.BITCOIN_WALLET);

        service = new IntegrationOrchestrationServiceImpl(
                List.of(bitcoinProvider),
                encryptionService,
                userRepository,
                credentialRepository,
                accountRepository,
                balanceRepository,
                snapshotRepository,
                objectMapper
        );
    }

    // -------------------------------------------------------------------------
    // connectProvider()
    // -------------------------------------------------------------------------

    @Nested
    class ConnectProvider {

        @Test
        void connectProvider_givenValidBitcoinCredentials_savesCredentialAndAccounts() {
            User user = buildUser(USER_ID);
            ConnectAccountRequest request = new ConnectAccountRequest(
                    ProviderType.BITCOIN_WALLET,
                    java.util.Map.of("address", EXTERNAL_ID)
            );
            IntegrationProvider.DiscoveredAccount discovered = new IntegrationProvider.DiscoveredAccount(
                    EXTERNAL_ID,
                    "Bitcoin Wallet (...mdq)",
                    AccountType.CRYPTO_WALLET,
                    "BTC",
                    new BigDecimal("1.00000000"),
                    new BigDecimal("60000.00")
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(credentialRepository.findByUserIdAndProviderType(USER_ID, ProviderType.BITCOIN_WALLET))
                    .thenReturn(Optional.empty());
            when(bitcoinProvider.connect(any())).thenReturn(List.of(discovered));
            when(encryptionService.encrypt(anyString())).thenReturn("encrypted-blob");

            IntegrationCredential savedCredential = buildCredential(CREDENTIAL_ID, user);
            when(credentialRepository.save(any(IntegrationCredential.class))).thenReturn(savedCredential);

            FinancialAccount savedAccount = buildAccount(ACCOUNT_ID, user, savedCredential);
            when(accountRepository.save(any(FinancialAccount.class))).thenReturn(savedAccount);

            FinancialAccountBalance savedBalance = buildBalance(
                    savedAccount, new BigDecimal("1.00000000"), new BigDecimal("60000.00"));
            when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(balanceRepository.save(any(FinancialAccountBalance.class))).thenReturn(savedBalance);
            when(snapshotRepository.save(any(BalanceSnapshot.class)))
                    .thenReturn(new BalanceSnapshot());

            List<AccountResponse> responses = service.connectProvider(USER_ID, request);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).credentialId()).isEqualTo(CREDENTIAL_ID);
            assertThat(responses.get(0).currency()).isEqualTo("BTC");
            verify(credentialRepository).save(any(IntegrationCredential.class));
            verify(accountRepository).save(any(FinancialAccount.class));
            verify(snapshotRepository).save(any(BalanceSnapshot.class));
        }

        @Test
        void connectProvider_givenAlreadyConnected_throwsIllegalArgument() {
            User user = buildUser(USER_ID);
            ConnectAccountRequest request = new ConnectAccountRequest(
                    ProviderType.BITCOIN_WALLET,
                    java.util.Map.of("address", EXTERNAL_ID)
            );

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(credentialRepository.findByUserIdAndProviderType(USER_ID, ProviderType.BITCOIN_WALLET))
                    .thenReturn(Optional.of(buildCredential(CREDENTIAL_ID, user)));

            assertThatThrownBy(() -> service.connectProvider(USER_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already connected");
        }

        @Test
        void connectProvider_givenUnknownUser_throwsResourceNotFoundException() {
            ConnectAccountRequest request = new ConnectAccountRequest(
                    ProviderType.BITCOIN_WALLET,
                    java.util.Map.of("address", EXTERNAL_ID)
            );
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.connectProvider(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getAccounts()
    // -------------------------------------------------------------------------

    @Nested
    class GetAccounts {

        @Test
        void getAccounts_givenActiveAccounts_returnsDtos() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            FinancialAccountBalance balance = buildBalance(
                    account, new BigDecimal("0.50000000"), new BigDecimal("30000.00"));

            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
            when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(balance));

            List<AccountResponse> responses = service.getAccounts(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo(ACCOUNT_ID);
            assertThat(responses.get(0).balance()).isEqualByComparingTo("0.50000000");
            assertThat(responses.get(0).balanceUsd()).isEqualByComparingTo("30000.00");
        }

        @Test
        void getAccounts_givenNoAccounts_returnsEmptyList() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<AccountResponse> responses = service.getAccounts(USER_ID);

            assertThat(responses).isEmpty();
        }

        @Test
        void getAccounts_givenAccountWithNoBalance_returnsNullBalanceFields() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
            when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            List<AccountResponse> responses = service.getAccounts(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).balance()).isNull();
            assertThat(responses.get(0).balanceUsd()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // getProviders()
    // -------------------------------------------------------------------------

    @Nested
    class GetProviders {

        @Test
        void getProviders_givenConnectedProviders_returnsStatuses() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            when(credentialRepository.findByUserId(USER_ID)).thenReturn(List.of(credential));
            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));

            List<ProviderStatusResponse> responses = service.getProviders(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).credentialId()).isEqualTo(CREDENTIAL_ID);
            assertThat(responses.get(0).providerType()).isEqualTo(ProviderType.BITCOIN_WALLET);
            assertThat(responses.get(0).status()).isEqualTo(SyncStatus.ACTIVE);
            assertThat(responses.get(0).accountCount()).isEqualTo(1);
        }

        @Test
        void getProviders_givenNoConnectedProviders_returnsEmptyList() {
            when(credentialRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            List<ProviderStatusResponse> responses = service.getProviders(USER_ID);

            assertThat(responses).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // disconnectProvider()
    // -------------------------------------------------------------------------

    @Nested
    class DisconnectProvider {

        @Test
        void disconnectProvider_givenValidCredential_softDeletesAccountsAndRevokesCredential() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            when(credentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
            when(accountRepository.findByCredentialId(CREDENTIAL_ID)).thenReturn(List.of(account));
            when(accountRepository.saveAll(anyList())).thenReturn(List.of(account));
            when(credentialRepository.save(any(IntegrationCredential.class))).thenReturn(credential);

            service.disconnectProvider(USER_ID, CREDENTIAL_ID);

            assertThat(account.isActive()).isFalse();
            ArgumentCaptor<IntegrationCredential> captor =
                    ArgumentCaptor.forClass(IntegrationCredential.class);
            verify(credentialRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SyncStatus.REVOKED);
            assertThat(captor.getValue().getCredentialData()).isNull();
        }

        @Test
        void disconnectProvider_givenCredentialNotFound_throwsResourceNotFoundException() {
            when(credentialRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.disconnectProvider(USER_ID, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        void disconnectProvider_givenWrongUser_throwsResourceNotFoundException() {
            User otherUser = buildUser(999L);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, otherUser);

            when(credentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));

            assertThatThrownBy(() -> service.disconnectProvider(USER_ID, CREDENTIAL_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // syncProvider()
    // -------------------------------------------------------------------------

    @Nested
    class SyncProvider {

        @Test
        void syncProvider_givenSuccessfulSync_updatesBalancesAndReturnsActiveStatus() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            IntegrationProvider.ProviderBalance providerBalance = new IntegrationProvider.ProviderBalance(
                    EXTERNAL_ID,
                    new BigDecimal("1.50000000"),
                    new BigDecimal("90000.00"),
                    Instant.now()
            );

            when(credentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
            when(encryptionService.decrypt("encrypted-blob")).thenReturn("{\"address\":\"" + EXTERNAL_ID + "\"}");
            when(bitcoinProvider.syncBalances(anyString())).thenReturn(List.of(providerBalance));
            when(accountRepository.findByCredentialId(CREDENTIAL_ID)).thenReturn(List.of(account));
            when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(balanceRepository.save(any(FinancialAccountBalance.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(snapshotRepository.save(any(BalanceSnapshot.class)))
                    .thenReturn(new BalanceSnapshot());
            when(credentialRepository.save(any(IntegrationCredential.class))).thenReturn(credential);

            SyncResultResponse result = service.syncProvider(USER_ID, CREDENTIAL_ID);

            assertThat(result.status()).isEqualTo(SyncStatus.ACTIVE);
            assertThat(result.accountsUpdated()).isEqualTo(1);
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        void syncProvider_givenProviderError_setsErrorStatus() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);

            when(credentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
            when(encryptionService.decrypt("encrypted-blob")).thenReturn("{\"address\":\"" + EXTERNAL_ID + "\"}");
            when(bitcoinProvider.syncBalances(anyString()))
                    .thenThrow(new ProviderException(ProviderType.BITCOIN_WALLET, "Blockstream timed out"));
            when(credentialRepository.save(any(IntegrationCredential.class))).thenReturn(credential);

            SyncResultResponse result = service.syncProvider(USER_ID, CREDENTIAL_ID);

            assertThat(result.status()).isEqualTo(SyncStatus.ERROR);
            assertThat(result.accountsUpdated()).isZero();
            assertThat(result.errorMessage()).isEqualTo("Sync failed. Please try again or reconnect the provider.");

            ArgumentCaptor<IntegrationCredential> captor =
                    ArgumentCaptor.forClass(IntegrationCredential.class);
            verify(credentialRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(SyncStatus.ERROR);
            assertThat(captor.getValue().getErrorMessage()).contains("Blockstream timed out");
        }

        @Test
        void syncProvider_givenUnknownExternalId_skipsAccountAndReturnsZeroUpdated() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);

            IntegrationProvider.ProviderBalance providerBalance = new IntegrationProvider.ProviderBalance(
                    "unknown-external-id",
                    new BigDecimal("1.00000000"),
                    new BigDecimal("60000.00"),
                    Instant.now()
            );

            when(credentialRepository.findById(CREDENTIAL_ID)).thenReturn(Optional.of(credential));
            when(encryptionService.decrypt("encrypted-blob")).thenReturn("{\"address\":\"" + EXTERNAL_ID + "\"}");
            when(bitcoinProvider.syncBalances(anyString())).thenReturn(List.of(providerBalance));
            // No account matches the returned externalId
            when(accountRepository.findByCredentialId(CREDENTIAL_ID)).thenReturn(Collections.emptyList());
            when(credentialRepository.save(any(IntegrationCredential.class))).thenReturn(credential);

            SyncResultResponse result = service.syncProvider(USER_ID, CREDENTIAL_ID);

            assertThat(result.status()).isEqualTo(SyncStatus.ACTIVE);
            assertThat(result.accountsUpdated()).isZero();
        }

        @Test
        void syncProvider_givenCredentialNotFound_throwsResourceNotFoundException() {
            when(credentialRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.syncProvider(USER_ID, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getNetWorth()
    // -------------------------------------------------------------------------

    @Nested
    class GetNetWorth {

        @Test
        void getNetWorth_givenMultipleAccounts_aggregatesCorrectly() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account1 = buildAccount(ACCOUNT_ID, user, credential);
            FinancialAccount account2 = buildAccount(ACCOUNT_ID + 1, user, credential);

            FinancialAccountBalance balance1 = buildBalance(
                    account1, new BigDecimal("1.00000000"), new BigDecimal("60000.00"));
            FinancialAccountBalance balance2 = buildBalance(
                    account2, new BigDecimal("0.50000000"), new BigDecimal("30000.00"));

            when(accountRepository.findByUserIdAndActiveTrue(USER_ID))
                    .thenReturn(List.of(account1, account2));
            when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(balance1));
            when(balanceRepository.findByAccountId(ACCOUNT_ID + 1)).thenReturn(Optional.of(balance2));

            NetWorthResponse response = service.getNetWorth(USER_ID);

            assertThat(response.totalNetWorthUsd()).isEqualByComparingTo("90000.00");
            assertThat(response.byProvider()).hasSize(1);
            assertThat(response.byProvider().get(0).providerType()).isEqualTo(ProviderType.BITCOIN_WALLET);
            assertThat(response.byProvider().get(0).totalUsd()).isEqualByComparingTo("90000.00");
            assertThat(response.byProvider().get(0).accountCount()).isEqualTo(2);
        }

        @Test
        void getNetWorth_givenNoActiveAccounts_returnsTotalZero() {
            when(accountRepository.findByUserIdAndActiveTrue(USER_ID))
                    .thenReturn(Collections.emptyList());

            NetWorthResponse response = service.getNetWorth(USER_ID);

            assertThat(response.totalNetWorthUsd()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.byProvider()).isEmpty();
            assertThat(response.byAccountType()).isEmpty();
        }

        @Test
        void getNetWorth_givenAccountWithMissingBalance_excludesFromTotal() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            when(accountRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(account));
            when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

            NetWorthResponse response = service.getNetWorth(USER_ID);

            assertThat(response.totalNetWorthUsd()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // -------------------------------------------------------------------------
    // getNetWorthHistory()
    // -------------------------------------------------------------------------

    @Nested
    class GetNetWorthHistory {

        @Test
        void getNetWorthHistory_givenSnapshotsInWindow_returnsDataPoints() {
            User user = buildUser(USER_ID);
            IntegrationCredential credential = buildCredential(CREDENTIAL_ID, user);
            FinancialAccount account = buildAccount(ACCOUNT_ID, user, credential);

            BalanceSnapshot snapshot = new BalanceSnapshot();
            snapshot.setAccount(account);
            snapshot.setBalance(new BigDecimal("1.00000000"));
            snapshot.setBalanceUsd(new BigDecimal("60000.00"));
            snapshot.setRecordedAt(Instant.now());

            when(accountRepository.findByUserId(USER_ID)).thenReturn(List.of(account));
            when(snapshotRepository.findByAccountIdAndRecordedAtAfterOrderByRecordedAtDesc(
                    eq(ACCOUNT_ID), any(Instant.class))).thenReturn(List.of(snapshot));

            NetWorthHistoryResponse response = service.getNetWorthHistory(USER_ID, 7);

            assertThat(response.dataPoints()).isNotEmpty();
            // 7 days of history produces at least 7 data points (inclusive range)
            assertThat(response.dataPoints().size()).isGreaterThanOrEqualTo(7);
        }

        @Test
        void getNetWorthHistory_givenNoAccounts_returnsDataPointsAllZero() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

            NetWorthHistoryResponse response = service.getNetWorthHistory(USER_ID, 30);

            assertThat(response.dataPoints()).isNotEmpty();
            assertThat(response.dataPoints()).allMatch(dp -> dp.totalUsd().compareTo(BigDecimal.ZERO) == 0);
        }
    }

    // -------------------------------------------------------------------------
    // Builder helpers
    // -------------------------------------------------------------------------

    private User buildUser(Long id) {
        return new User() {
            @Override
            public Long getId() { return id; }
        };
    }

    private IntegrationCredential buildCredential(Long id, User user) {
        IntegrationCredential credential = new IntegrationCredential() {
            @Override
            public Long getId() { return id; }
        };
        credential.setUser(user);
        credential.setProviderType(ProviderType.BITCOIN_WALLET);
        credential.setCredentialData("encrypted-blob");
        credential.setStatus(SyncStatus.ACTIVE);
        credential.setLastSyncedAt(Instant.now());
        return credential;
    }

    private FinancialAccount buildAccount(Long id, User user, IntegrationCredential credential) {
        FinancialAccount account = new FinancialAccount() {
            @Override
            public Long getId() { return id; }
        };
        account.setUser(user);
        account.setCredential(credential);
        account.setProviderType(ProviderType.BITCOIN_WALLET);
        account.setAccountType(AccountType.CRYPTO_WALLET);
        account.setExternalId(EXTERNAL_ID);
        account.setDisplayName("Bitcoin Wallet (...mdq)");
        account.setCurrency("BTC");
        account.setActive(true);
        return account;
    }

    private FinancialAccountBalance buildBalance(
            FinancialAccount account, BigDecimal balance, BigDecimal balanceUsd) {
        FinancialAccountBalance fab = new FinancialAccountBalance();
        fab.setAccount(account);
        fab.setBalance(balance);
        fab.setBalanceUsd(balanceUsd);
        fab.setAsOf(Instant.now());
        return fab;
    }
}
