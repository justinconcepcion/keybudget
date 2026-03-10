package com.keybudget.integration;

import com.keybudget.integration.dto.*;
import com.keybudget.integration.provider.plaid.PlaidService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** REST endpoints for financial provider integrations and net-worth queries. */
@Validated
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    /** Providers that may be connected through the Plaid Link flow. */
    private static final Set<ProviderType> PLAID_PROVIDERS = Set.of(
            ProviderType.M1_FINANCE,
            ProviderType.MARCUS
    );

    private final IntegrationOrchestrationService orchestrationService;
    private final PlaidService plaidService;

    public IntegrationController(
            IntegrationOrchestrationService orchestrationService,
            PlaidService plaidService) {
        this.orchestrationService = orchestrationService;
        this.plaidService = plaidService;
    }

    /**
     * POST /api/v1/integrations/connect
     * Connects a new external financial provider for the authenticated user.
     * Returns the list of discovered accounts with initial balances.
     */
    @PostMapping("/connect")
    public ResponseEntity<List<AccountResponse>> connectProvider(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectAccountRequest request) {
        Long userId = jwt.getClaim("userId");
        List<AccountResponse> accounts = orchestrationService.connectProvider(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts);
    }

    /**
     * GET /api/v1/integrations/accounts
     * Returns all financial accounts across every connected provider for the authenticated user.
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orchestrationService.getAccounts(userId));
    }

    /**
     * GET /api/v1/integrations/providers
     * Returns the connection status for every provider the authenticated user has connected.
     */
    @GetMapping("/providers")
    public ResponseEntity<List<ProviderStatusResponse>> getProviders(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orchestrationService.getProviders(userId));
    }

    /**
     * DELETE /api/v1/integrations/providers/{credentialId}
     * Disconnects a provider by revoking the credential and soft-deleting its accounts.
     */
    @DeleteMapping("/providers/{credentialId}")
    public ResponseEntity<Void> disconnectProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long credentialId) {
        Long userId = jwt.getClaim("userId");
        orchestrationService.disconnectProvider(userId, credentialId);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/integrations/providers/{credentialId}/sync
     * Manually triggers a balance sync for the specified provider credential.
     */
    @PostMapping("/providers/{credentialId}/sync")
    public ResponseEntity<SyncResultResponse> syncProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long credentialId) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orchestrationService.syncProvider(userId, credentialId));
    }

    /**
     * GET /api/v1/integrations/net-worth
     * Returns the user's current net worth aggregated across all active accounts.
     */
    @GetMapping("/net-worth")
    public ResponseEntity<NetWorthResponse> getNetWorth(
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orchestrationService.getNetWorth(userId));
    }

    /**
     * GET /api/v1/integrations/net-worth/history?days=30
     * Returns a time-series of the user's net worth in USD for the specified number of days.
     * Defaults to 30 days if the {@code days} parameter is omitted.
     */
    @GetMapping("/net-worth/history")
    public ResponseEntity<NetWorthHistoryResponse> getNetWorthHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orchestrationService.getNetWorthHistory(userId, days));
    }

    /**
     * POST /api/v1/integrations/plaid/link-token?provider=M1_FINANCE
     *
     * <p>Creates a Plaid Link token for the authenticated user and the specified provider.
     * The frontend passes this token to the Plaid Link SDK to open the institution-connection
     * UI. Only {@code M1_FINANCE} and {@code MARCUS} are accepted; any other value returns 400.
     *
     * @param jwt      the authenticated user's JWT
     * @param provider the target provider; must be {@code M1_FINANCE} or {@code MARCUS}
     * @return 200 with a {@link PlaidLinkTokenResponse} on success, 400 if provider is invalid
     */
    @PostMapping("/plaid/link-token")
    public ResponseEntity<PlaidLinkTokenResponse> createPlaidLinkToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam ProviderType provider) {

        if (!PLAID_PROVIDERS.contains(provider)) {
            throw new IllegalArgumentException(
                    "Provider " + provider + " does not support Plaid Link. "
                    + "Supported providers: M1_FINANCE, MARCUS");
        }

        Long userId = jwt.getClaim("userId");
        PlaidService.PlaidLinkTokenResult result = plaidService.createLinkToken(userId, provider);
        return ResponseEntity.ok(new PlaidLinkTokenResponse(result.linkToken(), result.expiration()));
    }

    /**
     * POST /api/v1/integrations/plaid/exchange
     *
     * <p>Exchanges the short-lived Plaid public token (returned by the frontend Link SDK after
     * the user connects their institution) for a permanent access token, then delegates to
     * the standard connect flow to persist the credential and discover accounts.
     *
     * @param jwt     the authenticated user's JWT
     * @param request contains the public token and the provider that was connected
     * @return 201 with the list of discovered and persisted accounts
     */
    @PostMapping("/plaid/exchange")
    public ResponseEntity<List<AccountResponse>> exchangePlaidPublicToken(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaidExchangeRequest request) {

        Long userId = jwt.getClaim("userId");

        PlaidService.PlaidAccessTokenResult tokenResult =
                plaidService.exchangePublicToken(request.publicToken());

        ConnectAccountRequest connectRequest = new ConnectAccountRequest(
                request.provider(),
                java.util.Map.of("plaidAccessToken", tokenResult.accessToken())
        );

        List<AccountResponse> accounts = orchestrationService.connectProvider(userId, connectRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts);
    }
}
