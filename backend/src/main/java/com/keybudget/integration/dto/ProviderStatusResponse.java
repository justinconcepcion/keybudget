package com.keybudget.integration.dto;

import com.keybudget.integration.ProviderType;
import com.keybudget.integration.SyncStatus;

import java.time.Instant;

/**
 * Response DTO summarising the current connection status of a single provider credential.
 */
public record ProviderStatusResponse(
    Long credentialId,
    ProviderType providerType,
    SyncStatus status,
    Instant lastSyncedAt,
    String errorMessage,
    int accountCount
) {}
