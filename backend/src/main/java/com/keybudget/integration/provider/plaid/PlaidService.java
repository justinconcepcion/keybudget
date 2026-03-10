package com.keybudget.integration.provider.plaid;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderException;

/**
 * Service contract for Plaid API interactions used by the Plaid Link flow.
 *
 * <p>Implementations are responsible for calling the Plaid REST API directly via WebClient.
 * The Plaid Java SDK is intentionally not used to keep the dependency surface minimal and
 * to remain in full control of HTTP error handling and timeout behaviour.
 */
public interface PlaidService {

    /**
     * Creates a Plaid Link token for the given user and provider combination.
     *
     * <p>The link token is a short-lived (30-minute) opaque token that the frontend
     * Plaid Link SDK uses to open the institution-connection UI. Each provider maps to
     * a distinct set of Plaid products:
     * <ul>
     *   <li>{@code M1_FINANCE} → {@code ["investments"]}</li>
     *   <li>{@code MARCUS}     → {@code ["auth", "transactions"]}</li>
     * </ul>
     *
     * @param userId   the authenticated user's database ID (used as Plaid's client_user_id)
     * @param provider the provider being connected; must be a Plaid-backed provider
     * @return a {@link PlaidLinkTokenResult} containing the link token and its expiration timestamp
     * @throws IllegalArgumentException if {@code provider} is not a Plaid-backed provider
     * @throws ProviderException        if the Plaid API returns an error or is unreachable
     */
    PlaidLinkTokenResult createLinkToken(Long userId, ProviderType provider);

    /**
     * Exchanges a short-lived Plaid public token (returned by the frontend Link SDK)
     * for a permanent access token.
     *
     * <p>The returned access token must be stored encrypted and used for all subsequent
     * Plaid API calls for this Item (user + institution link).
     *
     * @param publicToken the public token received from the Plaid Link SDK on the frontend
     * @return a {@link PlaidAccessTokenResult} containing the permanent access token and Item ID
     * @throws ProviderException if the Plaid API rejects the public token or is unreachable
     */
    PlaidAccessTokenResult exchangePublicToken(String publicToken);

    /**
     * Result of a successful Plaid link-token creation.
     *
     * @param linkToken  opaque token to pass to the Plaid Link SDK on the frontend
     * @param expiration ISO-8601 timestamp at which the link token expires
     */
    record PlaidLinkTokenResult(String linkToken, String expiration) {}

    /**
     * Result of a successful public-token exchange.
     *
     * @param accessToken permanent Plaid access token to be stored encrypted
     * @param itemId      Plaid Item ID — stable identifier for this user's institution link
     */
    record PlaidAccessTokenResult(String accessToken, String itemId) {}
}
