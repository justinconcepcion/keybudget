package com.keybudget.integration;

import com.keybudget.integration.dto.*;

import java.util.List;

/** Orchestrates all financial provider integration operations for a user. */
public interface IntegrationOrchestrationService {

    /**
     * Connects a new provider for the given user. Validates credentials with the provider,
     * discovers accounts, persists an encrypted {@code IntegrationCredential}, creates
     * {@code FinancialAccount} records, and stores initial balances.
     * Throws {@link IllegalArgumentException} if the provider is already connected.
     *
     * @param userId  the authenticated user's id
     * @param request the connect payload containing provider type and raw credentials
     * @return list of discovered and persisted accounts with initial balances
     */
    List<AccountResponse> connectProvider(Long userId, ConnectAccountRequest request);

    /**
     * Returns all financial accounts across all connected providers for the user,
     * each enriched with the latest known balance.
     *
     * @param userId the authenticated user's id
     * @return list of account response DTOs
     */
    List<AccountResponse> getAccounts(Long userId);

    /**
     * Returns the connection status of every provider the user has connected.
     *
     * @param userId the authenticated user's id
     * @return list of provider status DTOs with sync metadata and account counts
     */
    List<ProviderStatusResponse> getProviders(Long userId);

    /**
     * Disconnects a provider by soft-deleting all associated accounts and deleting
     * the credential record. Verifies that the credential belongs to the user.
     * Throws {@link com.keybudget.shared.ResourceNotFoundException} if the credential is not found
     * or does not belong to the user.
     *
     * @param userId       the authenticated user's id
     * @param credentialId the credential primary key to revoke
     */
    void disconnectProvider(Long userId, Long credentialId);

    /**
     * Manually triggers a balance sync for a specific provider credential.
     * Decrypts credentials, calls the provider, updates current balances, and writes
     * a new {@code BalanceSnapshot} for each account. Updates {@code SyncStatus} on the
     * credential to reflect success or failure.
     * Throws {@link com.keybudget.shared.ResourceNotFoundException} if the credential is not found
     * or does not belong to the user.
     *
     * @param userId       the authenticated user's id
     * @param credentialId the credential primary key to sync
     * @return the result of the sync operation
     */
    SyncResultResponse syncProvider(Long userId, Long credentialId);

    /**
     * Aggregates the latest USD balances across all active accounts for the user,
     * grouped by provider and by account type.
     *
     * @param userId the authenticated user's id
     * @return the net-worth snapshot with breakdowns
     */
    NetWorthResponse getNetWorth(Long userId);

    /**
     * Builds a time-series of the user's net worth in USD over the requested number of days,
     * derived from historical {@code BalanceSnapshot} records. Each data point represents
     * the sum of the last known snapshot per account on that calendar date.
     *
     * @param userId the authenticated user's id
     * @param days   the number of days of history to return (e.g., 30, 90, 365)
     * @return the net-worth history with one data point per day
     */
    NetWorthHistoryResponse getNetWorthHistory(Long userId, int days);
}
