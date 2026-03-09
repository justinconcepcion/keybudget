package com.keybudget.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.dto.*;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.integration.model.*;
import com.keybudget.integration.repository.*;
import com.keybudget.shared.ResourceNotFoundException;
import com.keybudget.shared.encryption.EncryptionService;
import com.keybudget.user.User;
import com.keybudget.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** Default implementation of {@link IntegrationOrchestrationService}. */
@Slf4j
@Service
public class IntegrationOrchestrationServiceImpl implements IntegrationOrchestrationService {

    private final Map<ProviderType, IntegrationProvider> providers;
    private final EncryptionService encryptionService;
    private final UserRepository userRepository;
    private final IntegrationCredentialRepository credentialRepository;
    private final FinancialAccountRepository accountRepository;
    private final FinancialAccountBalanceRepository balanceRepository;
    private final BalanceSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public IntegrationOrchestrationServiceImpl(
            List<IntegrationProvider> providerList,
            EncryptionService encryptionService,
            UserRepository userRepository,
            IntegrationCredentialRepository credentialRepository,
            FinancialAccountRepository accountRepository,
            FinancialAccountBalanceRepository balanceRepository,
            BalanceSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper) {
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
        // Build dispatch map from provider type to provider bean
        this.providers = providerList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        IntegrationProvider::getProviderType,
                        p -> p
                ));
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<AccountResponse> connectProvider(Long userId, ConnectAccountRequest request) {
        User user = resolveUser(userId);

        // Guard against duplicate connections
        credentialRepository.findByUserIdAndProviderType(userId, request.providerType())
                .ifPresent(c -> {
                    throw new IllegalArgumentException(
                            "Provider already connected: " + request.providerType());
                });

        IntegrationProvider provider = resolveProvider(request.providerType());

        // Delegate to provider — may throw ProviderAuthException / ProviderException
        List<IntegrationProvider.DiscoveredAccount> discovered =
                provider.connect(request.credentials());

        // Encrypt and persist credentials as a JSON blob
        String encryptedCredentialData = encryptCredentials(request.credentials());

        IntegrationCredential credential = new IntegrationCredential();
        credential.setUser(user);
        credential.setProviderType(request.providerType());
        credential.setCredentialData(encryptedCredentialData);
        credential.setStatus(SyncStatus.ACTIVE);
        credential.setLastSyncedAt(Instant.now());
        IntegrationCredential savedCredential = credentialRepository.save(credential);

        // Persist accounts and their initial balances
        List<AccountResponse> responses = new ArrayList<>();
        for (IntegrationProvider.DiscoveredAccount da : discovered) {
            FinancialAccount account = new FinancialAccount();
            account.setUser(user);
            account.setCredential(savedCredential);
            account.setProviderType(request.providerType());
            account.setAccountType(da.accountType());
            account.setExternalId(da.externalId());
            account.setDisplayName(da.displayName());
            account.setCurrency(da.currency());
            account.setActive(true);
            FinancialAccount savedAccount = accountRepository.save(account);

            FinancialAccountBalance balance = upsertBalance(
                    savedAccount, da.balance(), da.balanceUsd(), Instant.now());

            writeSnapshot(savedAccount, da.balance(), da.balanceUsd(), Instant.now());

            responses.add(toAccountResponse(savedAccount, balance));
        }

        log.info("Connected provider {} for userId={}, accounts={}", request.providerType(),
                userId, responses.size());
        return responses;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(Long userId) {
        List<FinancialAccount> accounts = accountRepository.findByUserId(userId);
        return accounts.stream()
                .map(account -> {
                    FinancialAccountBalance balance =
                            balanceRepository.findByAccountId(account.getId()).orElse(null);
                    return toAccountResponse(account, balance);
                })
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ProviderStatusResponse> getProviders(Long userId) {
        List<IntegrationCredential> credentials = credentialRepository.findByUserId(userId);

        // Count accounts per credential
        Map<Long, Long> accountCountByCredential = accountRepository.findByUserId(userId).stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCredential().getId(),
                        Collectors.counting()
                ));

        return credentials.stream()
                .map(c -> new ProviderStatusResponse(
                        c.getId(),
                        c.getProviderType(),
                        c.getStatus(),
                        c.getLastSyncedAt(),
                        c.getErrorMessage(),
                        accountCountByCredential.getOrDefault(c.getId(), 0L).intValue()
                ))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void disconnectProvider(Long userId, Long credentialId) {
        IntegrationCredential credential = resolveCredential(userId, credentialId);

        // Soft-delete all accounts under this credential
        List<FinancialAccount> accounts =
                accountRepository.findByCredentialId(credentialId);
        accounts.forEach(a -> a.setActive(false));
        accountRepository.saveAll(accounts);

        // Mark credential as revoked rather than deleting — preserves audit trail
        credential.setStatus(SyncStatus.REVOKED);
        credentialRepository.save(credential);

        log.info("Disconnected provider {} for userId={}", credential.getProviderType(), userId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SyncResultResponse syncProvider(Long userId, Long credentialId) {
        IntegrationCredential credential = resolveCredential(userId, credentialId);

        IntegrationProvider provider = resolveProvider(credential.getProviderType());
        String decryptedData = encryptionService.decrypt(credential.getCredentialData());

        Instant syncedAt = Instant.now();
        int accountsUpdated = 0;

        try {
            List<IntegrationProvider.ProviderBalance> providerBalances =
                    provider.syncBalances(decryptedData);

            // Index existing accounts by externalId for efficient lookup
            Map<String, FinancialAccount> accountsByExternalId =
                    accountRepository.findByCredentialId(credentialId).stream()
                            .collect(Collectors.toMap(
                                    FinancialAccount::getExternalId,
                                    a -> a
                            ));

            for (IntegrationProvider.ProviderBalance pb : providerBalances) {
                FinancialAccount account = accountsByExternalId.get(pb.externalId());
                if (account == null) {
                    log.warn("Received balance for unknown externalId={} on credentialId={}",
                            pb.externalId(), credentialId);
                    continue;
                }
                upsertBalance(account, pb.balance(), pb.balanceUsd(), pb.asOf());
                writeSnapshot(account, pb.balance(), pb.balanceUsd(), pb.asOf());
                accountsUpdated++;
            }

            credential.setStatus(SyncStatus.ACTIVE);
            credential.setLastSyncedAt(syncedAt);
            credential.setErrorMessage(null);
            credentialRepository.save(credential);

            log.info("Synced provider {} for userId={}, accountsUpdated={}",
                    credential.getProviderType(), userId, accountsUpdated);

            return new SyncResultResponse(
                    credential.getProviderType(), syncedAt, accountsUpdated,
                    SyncStatus.ACTIVE, null);

        } catch (ProviderException ex) {
            log.error("Sync failed for provider {} userId={}: {}",
                    credential.getProviderType(), userId, ex.getMessage(), ex);
            credential.setStatus(SyncStatus.ERROR);
            credential.setErrorMessage(truncate(ex.getMessage(), 500));
            credentialRepository.save(credential);

            return new SyncResultResponse(
                    credential.getProviderType(), syncedAt, 0,
                    SyncStatus.ERROR, truncate(ex.getMessage(), 500));
        }
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public NetWorthResponse getNetWorth(Long userId) {
        List<FinancialAccount> activeAccounts =
                accountRepository.findByUserIdAndActiveTrue(userId);

        Instant asOf = Instant.now();
        BigDecimal total = BigDecimal.ZERO;

        Map<ProviderType, BigDecimal> providerTotals = new EnumMap<>(ProviderType.class);
        Map<ProviderType, Integer> providerCounts = new EnumMap<>(ProviderType.class);
        Map<AccountType, BigDecimal> typeTotals = new EnumMap<>(AccountType.class);
        Map<AccountType, Integer> typeCounts = new EnumMap<>(AccountType.class);

        for (FinancialAccount account : activeAccounts) {
            Optional<FinancialAccountBalance> balanceOpt =
                    balanceRepository.findByAccountId(account.getId());
            if (balanceOpt.isEmpty()) {
                continue;
            }
            BigDecimal usd = balanceOpt.get().getBalanceUsd();

            total = total.add(usd);

            providerTotals.merge(account.getProviderType(), usd, BigDecimal::add);
            providerCounts.merge(account.getProviderType(), 1, Integer::sum);

            typeTotals.merge(account.getAccountType(), usd, BigDecimal::add);
            typeCounts.merge(account.getAccountType(), 1, Integer::sum);
        }

        List<ProviderTotal> byProvider = providerTotals.entrySet().stream()
                .map(e -> new ProviderTotal(
                        e.getKey(), e.getValue(),
                        providerCounts.getOrDefault(e.getKey(), 0)))
                .toList();

        List<AccountTypeTotal> byAccountType = typeTotals.entrySet().stream()
                .map(e -> new AccountTypeTotal(
                        e.getKey(), e.getValue(),
                        typeCounts.getOrDefault(e.getKey(), 0)))
                .toList();

        return new NetWorthResponse(total, byProvider, byAccountType, asOf);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public NetWorthHistoryResponse getNetWorthHistory(Long userId, int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        List<FinancialAccount> accounts = accountRepository.findByUserId(userId);

        // Collect all snapshots within the window grouped by accountId
        Map<Long, List<BalanceSnapshot>> snapshotsByAccount = new HashMap<>();
        for (FinancialAccount account : accounts) {
            List<BalanceSnapshot> snapshots =
                    snapshotRepository.findByAccountIdAndRecordedAtAfterOrderByRecordedAtDesc(
                            account.getId(), cutoff);
            if (!snapshots.isEmpty()) {
                snapshotsByAccount.put(account.getId(), snapshots);
            }
        }

        // Build the series of calendar dates from cutoff to today
        LocalDate startDate = cutoff.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);

        List<NetWorthDataPoint> dataPoints = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            BigDecimal dailyTotal = aggregateDailyTotal(snapshotsByAccount, date);
            dataPoints.add(new NetWorthDataPoint(date, dailyTotal));
        }

        return new NetWorthHistoryResponse(dataPoints);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private IntegrationProvider resolveProvider(ProviderType providerType) {
        IntegrationProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("No provider registered for: " + providerType);
        }
        return provider;
    }

    private IntegrationCredential resolveCredential(Long userId, Long credentialId) {
        IntegrationCredential credential = credentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Credential not found: " + credentialId));
        if (!credential.getUser().getId().equals(userId)) {
            // Treat as not found to avoid leaking credential existence
            throw new ResourceNotFoundException("Credential not found: " + credentialId);
        }
        return credential;
    }

    private String encryptCredentials(Map<String, String> credentials) {
        try {
            String json = objectMapper.writeValueAsString(credentials);
            return encryptionService.encrypt(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize credentials", ex);
        }
    }

    private FinancialAccountBalance upsertBalance(
            FinancialAccount account,
            BigDecimal balance,
            BigDecimal balanceUsd,
            Instant asOf) {
        FinancialAccountBalance fab = balanceRepository.findByAccountId(account.getId())
                .orElseGet(() -> {
                    FinancialAccountBalance b = new FinancialAccountBalance();
                    b.setAccount(account);
                    return b;
                });
        fab.setBalance(balance);
        fab.setBalanceUsd(balanceUsd);
        fab.setAsOf(asOf);
        return balanceRepository.save(fab);
    }

    private void writeSnapshot(
            FinancialAccount account,
            BigDecimal balance,
            BigDecimal balanceUsd,
            Instant recordedAt) {
        BalanceSnapshot snapshot = new BalanceSnapshot();
        snapshot.setAccount(account);
        snapshot.setBalance(balance);
        snapshot.setBalanceUsd(balanceUsd);
        snapshot.setRecordedAt(recordedAt);
        snapshotRepository.save(snapshot);
    }

    private AccountResponse toAccountResponse(
            FinancialAccount account,
            FinancialAccountBalance balance) {
        BigDecimal bal = balance != null ? balance.getBalance() : null;
        BigDecimal balUsd = balance != null ? balance.getBalanceUsd() : null;
        Instant asOf = balance != null ? balance.getAsOf() : null;
        return new AccountResponse(
                account.getId(),
                account.getCredential().getId(),
                account.getProviderType(),
                account.getAccountType(),
                account.getDisplayName(),
                account.getCurrency(),
                bal,
                balUsd,
                asOf,
                account.isActive()
        );
    }

    /**
     * For a given calendar date, sums the most recent snapshot per account that was
     * recorded on or before the end of that date.
     */
    private BigDecimal aggregateDailyTotal(
            Map<Long, List<BalanceSnapshot>> snapshotsByAccount,
            LocalDate date) {
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal total = BigDecimal.ZERO;
        for (List<BalanceSnapshot> snapshots : snapshotsByAccount.values()) {
            // Snapshots are ordered desc; find the first one on or before end of day
            for (BalanceSnapshot snapshot : snapshots) {
                if (!snapshot.getRecordedAt().isAfter(endOfDay)) {
                    total = total.add(snapshot.getBalanceUsd());
                    break;
                }
            }
        }
        return total;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
