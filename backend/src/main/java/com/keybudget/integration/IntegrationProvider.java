package com.keybudget.integration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Contract for an external financial data provider.
 * Each implementation handles a specific provider (Coinbase, Bitcoin wallet, etc.) and is
 * registered as a Spring-managed {@code @Service} bean. The orchestration service collects
 * all implementations into a {@code Map<ProviderType, IntegrationProvider>} for dispatch.
 *
 * <p>Design note: Java sealed interfaces require all permitted subclasses to reside in the
 * same package. Because each provider implementation lives in its own subpackage for future
 * growth, this interface is intentionally non-sealed. The provider registry pattern enforced
 * by the orchestration service provides equivalent closed-set safety at runtime.
 */
public interface IntegrationProvider {

    /**
     * Returns the {@link ProviderType} this implementation handles.
     *
     * @return the provider type
     */
    ProviderType getProviderType();

    /**
     * Validates the supplied raw credentials and, if valid, discovers all accounts
     * available under those credentials.
     *
     * @param credentials raw key/value credential map from the connect request
     * @return list of discovered accounts with initial balance data
     * @throws com.keybudget.integration.exception.ProviderAuthException    if credentials are invalid
     * @throws com.keybudget.integration.exception.ProviderRateLimitException if the provider rate-limits the request
     * @throws com.keybudget.integration.exception.ProviderException         on any other provider error
     */
    List<DiscoveredAccount> connect(Map<String, String> credentials);

    /**
     * Fetches the latest balances for all accounts reachable with the given credentials.
     *
     * @param decryptedCredentialData the decrypted JSON credential blob stored at connect time
     * @return list of balances keyed by the provider's external account identifier
     * @throws com.keybudget.integration.exception.ProviderAuthException    if the stored credential has expired
     * @throws com.keybudget.integration.exception.ProviderRateLimitException if the provider rate-limits the request
     * @throws com.keybudget.integration.exception.ProviderException         on any other provider error
     */
    List<ProviderBalance> syncBalances(String decryptedCredentialData);

    /**
     * Performs a lightweight check to determine whether the stored credential is still valid,
     * without fetching full balance data.
     *
     * @param decryptedCredentialData the decrypted JSON credential blob
     * @return {@code true} if the credential is still accepted by the provider, {@code false} otherwise
     */
    boolean validateCredential(String decryptedCredentialData);

    // -------------------------------------------------------------------------
    // Internal value types
    // -------------------------------------------------------------------------

    /**
     * An account discovered during the {@link #connect} call, before it has been persisted.
     *
     * @param externalId  the provider's own identifier for this account
     * @param displayName human-readable account name
     * @param accountType the type of account
     * @param currency    ISO currency code or crypto ticker (e.g., "USD", "BTC")
     * @param balance     native-currency balance
     * @param balanceUsd  USD equivalent at discovery time
     */
    record DiscoveredAccount(
            String externalId,
            String displayName,
            AccountType accountType,
            String currency,
            BigDecimal balance,
            BigDecimal balanceUsd
    ) {}

    /**
     * A balance reading returned by {@link #syncBalances}.
     *
     * @param externalId the provider's own account identifier, matching {@link DiscoveredAccount#externalId()}
     * @param balance    native-currency balance
     * @param balanceUsd USD equivalent at the time the provider reported this balance
     * @param asOf       timestamp at which the provider reported this balance
     */
    record ProviderBalance(
            String externalId,
            BigDecimal balance,
            BigDecimal balanceUsd,
            Instant asOf
    ) {}
}
