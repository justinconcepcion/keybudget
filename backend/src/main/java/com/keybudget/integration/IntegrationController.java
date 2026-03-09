package com.keybudget.integration;

import com.keybudget.integration.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST endpoints for financial provider integrations and net-worth queries. */
@Validated
@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationController {

    private final IntegrationOrchestrationService orchestrationService;

    public IntegrationController(IntegrationOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
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
            @RequestParam(defaultValue = "30") int days) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(orchestrationService.getNetWorthHistory(userId, days));
    }
}
